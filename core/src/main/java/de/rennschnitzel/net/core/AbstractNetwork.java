package de.rennschnitzel.net.core;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

import org.nustaq.serialization.FSTConfiguration;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;

import de.rennschnitzel.net.ProtocolUtils;
import de.rennschnitzel.net.core.Node.HomeNode;
import de.rennschnitzel.net.core.procedure.ProcedureCall;
import de.rennschnitzel.net.core.tunnel.SubTunnel;
import de.rennschnitzel.net.core.tunnel.SubTunnelDescriptor;
import de.rennschnitzel.net.core.tunnel.TunnelMessage;
import de.rennschnitzel.net.event.NodeEvent;
import de.rennschnitzel.net.exception.ConnectionException;
import de.rennschnitzel.net.exception.ProtocolException;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeTopologyMessage;
import de.rennschnitzel.net.protocol.TransportProtocol;
import de.rennschnitzel.net.protocol.TransportProtocol.ErrorMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureResponseMessage;
import de.rennschnitzel.net.util.concurrent.CloseableLock;
import de.rennschnitzel.net.util.concurrent.CloseableReadWriteLock;
import de.rennschnitzel.net.util.concurrent.ReentrantCloseableLock;
import de.rennschnitzel.net.util.concurrent.ReentrantCloseableReadWriteLock;
import lombok.Getter;
import lombok.NonNull;

public abstract class AbstractNetwork {

  public static FSTConfiguration SERIALIZATION = FSTConfiguration.createDefaultConfiguration();

  private static @Getter @NonNull AbstractNetwork instance = null;

  // static end
  // ******************************************************************************************



  private final @Getter HomeNode home;
  private final LoadingCache<UUID, Node> nodesCache = CacheBuilder.newBuilder().weakValues().build(CacheLoader.from(Node::new));
  private final Map<UUID, Node> nodes = new HashMap<>();
  private final CloseableReadWriteLock nodeLock = new ReentrantCloseableReadWriteLock();

  private final @Getter ProcedureManager procedureManager = new ProcedureManager(this);

  private final CloseableLock tunnelLock = new ReentrantCloseableLock();
  private final ConcurrentMap<String, Tunnel> tunnelsByName = new ConcurrentHashMap<>();
  private final ConcurrentMap<Integer, Tunnel> tunnelsById = new ConcurrentHashMap<>();
  private final ConcurrentMap<SubTunnelDescriptor<?>, SubTunnel> subTunnels = new ConcurrentHashMap<>();

  private final @Getter EventBus eventBus = new EventBus();

  protected AbstractNetwork(HomeNode home) {
    Preconditions.checkNotNull(home);
    home.setNetwork(this);
    this.home = home;
    this.nodes.put(home.getId(), home);
    // this.nodesCache.put(home.getId(), home);
    AbstractNetwork.instance = this;
  }

  protected void setInstance(AbstractNetwork instance) {
    Preconditions.checkNotNull(instance);
    AbstractNetwork.instance = instance;
  }

  public abstract Logger getLogger();

  public abstract ScheduledExecutorService getExecutor();

  // ***************************************************************************
  // Sending

  protected abstract <T, R> void sendProcedureCall(ProcedureCall<T, R> call);

  protected abstract void sendProcedureResponse(final UUID receiverId, final ProcedureResponseMessage msg) throws ProtocolException;

  protected abstract void sendProcedureResponse(final UUID senderId, final UUID receiverId, final ProcedureResponseMessage msg)
      throws ProtocolException;


  protected abstract boolean publishHomeNodeUpdate();


  // ***************************************************************************
  // CONNECTIONS

  protected abstract void addConnection(Connection connection);

  protected abstract void removeConnection(Connection connection);

  // ***************************************************************************
  // TUNNELS

  protected abstract boolean sendTunnelMessage(TunnelMessage cmsg);

  protected abstract boolean registerTunnel(Tunnel tunnel);

  /**
   * Gets a set of all registered tunnels.
   * 
   * @return set of tunnels
   */
  public Set<Tunnel> getTunnels() {
    try (CloseableLock l = tunnelLock.open()) {
      return ImmutableSet.copyOf(this.tunnelsByName.values());
    }
  }

  /**
   * Gets a set of all registered sub-tunnels. A sub-tunnel is a higher advanced layer.
   * 
   * @return set of sub-tunnels
   */
  public Set<SubTunnel> getSubTunnels() {
    try (CloseableLock l = tunnelLock.open()) {
      return ImmutableSet.copyOf(this.subTunnels.values());
    }
  }

  /**
   * Gets the tunnel with the given name if present
   * 
   * @param name of tunnel
   * @return null if not present
   */
  public Tunnel getTunnelIfPresent(String name) {
    return this.tunnelsByName.get(name.toLowerCase());
  }

  /**
   * Gets the tunnel with the given name. If the tunnel does not already exist a new Tunnel is
   * created and registered.
   * 
   * @param name of tunnel
   * @return tunnel
   */
  public Tunnel getTunnel(String name) {
    return getTunnel(name, true);
  }

  /**
   * Gets the tunnel with the given id.
   * 
   * @param tunnelId of tunnel
   * @return null if the tunnel does not exists
   */
  public Tunnel getTunnelById(int tunnelId) {
    return this.tunnelsById.get(tunnelId);
  }

  private Tunnel getTunnel(String name, boolean register) {
    final String key = name.toLowerCase();
    Tunnel tunnel = this.tunnelsByName.get(key);
    if (tunnel == null) {
      try (CloseableLock l = tunnelLock.open()) {
        // Check again, but in synchronized state!
        tunnel = this.tunnelsByName.get(key);
        if (tunnel == null) {
          tunnel = new Tunnel(this, key);
          this.tunnelsByName.put(tunnel.getName(), tunnel);
          this.tunnelsById.put(tunnel.getId(), tunnel);
          if (register) {
            this.registerTunnel(tunnel);
          }
        }
      }
    }
    return tunnel;
  }

  /**
   * Gets the sub-tunnel that is described by the descriptor if already present.
   * 
   * @param descriptor that describes the sub-tunnel
   * @return sub-tunnel
   */
  public <S extends SubTunnel> S getTunnelIfPresent(SubTunnelDescriptor<S> descriptor) {
    Preconditions.checkNotNull(descriptor);
    return descriptor.cast(this.subTunnels.get(descriptor));
  }

  /**
   * Gets the sub-tunnel that is described by the descriptor. If none exists a new sub-tunnel is
   * created and registered.
   * 
   * @param descriptor that describes the sub-tunnel
   * @return sub-tunnel
   */
  public <S extends SubTunnel> S getTunnel(SubTunnelDescriptor<S> descriptor) {
    Preconditions.checkNotNull(descriptor);
    S subTunnel = getTunnelIfPresent(descriptor);
    if (subTunnel == null) {
      try (CloseableLock l = tunnelLock.open()) {
        // Check again, but in synchronized state!
        subTunnel = getTunnelIfPresent(descriptor);
        if (subTunnel == null) {
          Tunnel tunnel = getTunnel(descriptor.getName(), false);
          subTunnel = descriptor.create(tunnel);
          this.subTunnels.put(descriptor, subTunnel);
          registerTunnel(tunnel);
        }
      }
    }
    return subTunnel;
  }

  protected void receiveTunnelRegister(TransportProtocol.TunnelRegister msg) throws ConnectionException {
    try (CloseableLock l = tunnelLock.open()) {
      Tunnel old = this.tunnelsByName.get(msg.getName());

      if (old != null) {

        if (old.getId() != msg.getTunnelId()) {
          throw new ConnectionException(ErrorMessage.Type.ID_ALREADY_USED, "Can't register Tunnel with id " + msg.getTunnelId()
              + " and name \"" + msg.getName() + "\". Already as \"" + old.getName() + "\" registered!");
        }
        old.setType(msg.getType());

      } else {

        Tunnel tunnel = new Tunnel(this, msg.getName());

        Preconditions.checkState(tunnel.getId() == msg.getTunnelId());
        tunnel.setType(msg.getType());
        this.tunnelsByName.put(tunnel.getName(), tunnel);
        this.tunnelsById.put(tunnel.getId(), tunnel);
      }
    }
  }


  // ***************************************************************************
  // Nodes

  /**
   * Get the nodes in the network.
   * 
   * @return set of nodes
   */
  public Set<Node> getNodes() {
    try (CloseableLock l = nodeLock.readLock().open()) {
      return ImmutableSet.copyOf(nodes.values());
    }
  }

  /**
   * Get the nodes that are described in the target object.
   * 
   * @param target describes a group of nodes
   * @return set of nodes
   */
  public Set<Node> getNodes(Target target) {
    try (CloseableLock l = nodeLock.readLock().open()) {
      ImmutableSet.Builder<Node> b = ImmutableSet.builder();
      this.nodes.values().stream().filter(target::contains).forEach(b::add);
      return b.build();
    }
  }

  /**
   * Gets the node with the given id.
   * 
   * @param id of node
   * @return node
   */
  public Node getNode(UUID id) {
    return this.nodes.get(id);
  }

  /**
   * Gets the cached node with the given id. Be aware that this node does not need to be existing in
   * the network!!! It automatically creates a new node if not present.
   * 
   * @param id of node
   * @return node
   */
  public Node getNodeUnsafe(UUID id) {
    return this.nodesCache.getUnchecked(id);
  }

  /**
   * Gets the node with the given name.
   * 
   * @param name of node
   * @return node, or null if not found
   */
  public Node getNode(String name) {
    try (CloseableLock l = nodeLock.readLock().open()) {
      return this.nodes.values().stream().filter(n -> n.getName().isPresent() && n.getName().get().equalsIgnoreCase(name)).findAny()
          .orElse(null);
    }
  }

  /**
   * Gets the nodes with the namespace
   * 
   * @param namespace of nodes
   * @return set of nodes
   */
  public Set<Node> getNodes(Namespace namespace) {
    return getNodesOfNamespace(namespace.getName());
  }

  /**
   * Gets the accumulation of nodes that belongs to the given namespaces.
   * 
   * @param namespace first
   * @param namespaces other
   * @return set of nodes
   */
  public Set<Node> getNodes(Namespace namespace, Namespace... namespaces) {
    return getNodesOfNamespace(namespace.getName(), Arrays.stream(namespaces).map(Namespace::getName).toArray(len -> new String[len]));
  }

  /**
   * Gets the accumulation nodes of the given namespaces.
   * 
   * @param namespaces
   * @return set of nodes
   */
  public Set<Node> getNodes(Collection<Namespace> namespaces) {
    try (CloseableLock l = nodeLock.readLock().open()) {
      ImmutableSet.Builder<Node> b = ImmutableSet.builder();
      for (Namespace namespace : namespaces) {
        this.nodes.values().stream().filter(n -> n.hasNamespace(namespace)).forEach(b::add);
      }
      return b.build();
    }
  }

  /**
   * Gets the accumulation of nodes that belongs to the given namespaces.
   * 
   * @param namespace - name of first namespace
   * @param namespaces - names of other namespaces
   * @return set of nodes
   */
  public Set<Node> getNodesOfNamespace(String namespace, String... namespaces) {
    try (CloseableLock l = nodeLock.readLock().open()) {
      ImmutableSet.Builder<Node> b = ImmutableSet.builder();
      this.nodes.values().stream().filter(n -> n.hasNamespace(namespace)).forEach(b::add);
      for (int i = 0; i < namespaces.length; ++i) {
        String s = namespaces[i];
        this.nodes.values().stream().filter(n -> n.hasNamespace(s)).forEach(b::add);
      }
      return b.build();
    }
  }

  /**
   * Gets the namespace object of a namespace name.
   * 
   * @param namespace name
   * @return namespace object
   */
  public Namespace getNamespace(String namespace) {
    return new Namespace(this, namespace);
  }

  /**
   * Updates a node from a received protocol message. DONT USE THIS!
   * 
   * @param msg - update message
   * @return node that is updated
   */
  public Node updateNode(NodeMessage msg) {
    UUID id = ProtocolUtils.convert(msg.getId());
    if (id.equals(this.getHome().getId())) {
      throw new IllegalArgumentException("Forbidden to update home node!");
    }
    return updateNodeSilent(msg);
  }

  private Node updateNodeSilent(NodeMessage msg) {
    UUID id = ProtocolUtils.convert(msg.getId());
    if (id.equals(this.getHome().getId())) {
      return this.getHome();
    }
    Node node = nodesCache.getUnchecked(id);
    node.update(msg);
    Node old = this.nodes.put(id, node);
    if (old == null) {
      this.eventBus.post(new NodeEvent.NetworkNodeAddedEvent(this, node));
    } else {
      this.eventBus.post(new NodeEvent.NetworkNodeUpdatedEvent(this, node));
    }
    return node;
  }

  /**
   * Updates all nodes from a received protocol message. DONT USE THIS!
   * 
   * @param msg - topology message
   */
  public void updateNodes(NodeTopologyMessage msg) {
    try (CloseableLock l = nodeLock.readLock().open()) {
      Set<Node> retain = Sets.newHashSet();
      for (NodeMessage node : msg.getNodesList()) {
        retain.add(updateNodeSilent(node));
      }
      retain.add(this.getHome());
      this.nodes.values().retainAll(retain);
    }
  }

  public void removeNode(UUID id) {
    if (id.equals(this.getHome().getId())) {
      throw new IllegalArgumentException("Forbidden to remove home node!");
    }
    Node node = this.nodes.remove(id);
    if (node != null) {
      node.disconnected();
      this.eventBus.post(new NodeEvent.NetworkNodeRemovedEvent(this, node));
    }
  }

  public NodeTopologyMessage getTopologyMessage() {
    NodeTopologyMessage.Builder b = NodeTopologyMessage.newBuilder();
    try (CloseableLock l = nodeLock.readLock().open()) {
      for (Node node : this.nodes.values()) {
        b.addNodes(node.toProtocol());
      }
    }
    return b.build();
  }

}
