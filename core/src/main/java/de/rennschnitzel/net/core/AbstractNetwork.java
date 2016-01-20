package de.rennschnitzel.net.core;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
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
import de.rennschnitzel.net.event.NetworkNodeEvent;
import de.rennschnitzel.net.protocol.ComponentsProtocol.UUIDMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeTopologyMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureResponseMessage;
import de.rennschnitzel.net.util.concurrent.CloseableLock;
import de.rennschnitzel.net.util.concurrent.CloseableReadWriteLock;
import de.rennschnitzel.net.util.concurrent.ReentrantCloseableReadWriteLock;
import lombok.Getter;
import lombok.NonNull;

public abstract class AbstractNetwork {

  public static FSTConfiguration SERIALIZATION = FSTConfiguration.createDefaultConfiguration();

  @Getter
  @NonNull
  private static AbstractNetwork instance = null;

  // static end
  // ******************************************************************************************


  @Getter
  private final HomeNode home;
  private final LoadingCache<UUID, Node> nodesCache = CacheBuilder.newBuilder().weakValues().build(CacheLoader.from(Node::new));
  private final Map<UUID, Node> nodes = new HashMap<>();
  private final CloseableReadWriteLock nodeLock = new ReentrantCloseableReadWriteLock();

  @Getter
  private final ProcedureManager procedureManager = new ProcedureManager(this);

  @Getter
  private final EventBus eventBus = new EventBus();

  protected AbstractNetwork(HomeNode home) {
    Preconditions.checkNotNull(home);
    home.setNetwork(this);
    this.home = home;
    this.nodes.put(home.getId(), home);
    // this.nodesCache.put(home.getId(), home);
    AbstractNetwork.instance = this;
  }

  public abstract Logger getLogger();

  public abstract ScheduledFuture<?> scheduleAsyncLater(Runnable run, long timeout, TimeUnit unit);

  // ***************************************************************************
  // Connection (sending)

  protected abstract <T, R> void sendProcedureCall(ProcedureCall<T, R> call) throws IOException;

  protected abstract void sendProcedureResponse(UUID receiver, ProcedureResponseMessage build) throws IOException;

  protected void sendProcedureResponse(UUIDMessage receiver, ProcedureResponseMessage build) throws IOException {
    sendProcedureResponse(ProtocolUtils.convert(receiver), build);
  }

  protected abstract void sendHomeNodeUpdate() throws IOException;

  // ***************************************************************************
  // Nodes

  public Set<Node> getNodes() {
    try (CloseableLock l = nodeLock.readLock().open()) {
      return ImmutableSet.copyOf(nodes.values());
    }
  }

  public Set<Node> getNodes(Target target) {
    try (CloseableLock l = nodeLock.readLock().open()) {
      ImmutableSet.Builder<Node> b = ImmutableSet.builder();
      this.nodes.values().stream().filter(target::contains).forEach(b::add);
      return b.build();
    }
  }

  public Node getNode(UUID id) {
    return this.nodes.get(id);
  }

  public Node getNode(String name) {
    try (CloseableLock l = nodeLock.readLock().open()) {
      return this.nodes.values().stream().filter(n -> n.getName().isPresent() && n.getName().get().equalsIgnoreCase(name)).findAny()
          .orElse(null);
    }
  }

  public Set<Node> getNodes(Namespace namespace) {
    return getNodesOfNamespace(namespace.getName());
  }

  public Set<Node> getNodes(Namespace namespace, Namespace... namespaces) {
    return getNodesOfNamespace(namespace.getName(), Arrays.stream(namespaces).map(Namespace::getName).toArray(len -> new String[len]));
  }

  public Set<Node> getNodes(Collection<Namespace> namespaces) {
    try (CloseableLock l = nodeLock.readLock().open()) {
      ImmutableSet.Builder<Node> b = ImmutableSet.builder();
      for (Namespace namespace : namespaces) {
        this.nodes.values().stream().filter(n -> n.hasNamespace(namespace)).forEach(b::add);
      }
      return b.build();
    }
  }

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

  public Namespace getNamespace(String namespace) {
    return new Namespace(this, namespace);
  }

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
    node.connected(msg);
    Node old = this.nodes.put(id, node);
    if (old == null) {
      this.eventBus.post(new NetworkNodeEvent.NetworkNodeAddedEvent(this, node));
    } else {
      this.eventBus.post(new NetworkNodeEvent.NetworkNodeUpdatedEvent(this, node));
    }
    return node;
  }

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
      this.eventBus.post(new NetworkNodeEvent.NetworkNodeRemovedEvent(this, node));
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
