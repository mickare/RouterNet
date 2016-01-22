package de.rennschnitzel.net.core;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import de.rennschnitzel.net.ProtocolUtils;
import de.rennschnitzel.net.core.procedure.Procedure;
import de.rennschnitzel.net.core.procedure.CallableRegisteredProcedure;
import de.rennschnitzel.net.protocol.TransportProtocol;
import de.rennschnitzel.net.protocol.NetworkProtocol.DataBukkitMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.DataBungeecordMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.DataRouterMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeMessage.Type;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public class Node {


  @Getter
  private volatile boolean connected = false;

  @NonNull
  @Getter
  private final UUID id;
  @Getter
  protected NodeMessage.Type type = NodeMessage.Type.UNRECOGNIZED;
  @Getter
  protected Optional<String> name = Optional.empty();
  protected final Set<String> namespaces = Collections.synchronizedSet(new HashSet<>());
  @Getter
  protected long startTimestamp = -1;
  // protected final Set<ProcedureInformation> procedures = Collections.synchronizedSet(new
  // HashSet<>());
  protected final Set<Procedure> procedures = Collections.synchronizedSet(Sets.newTreeSet());

  @Getter
  private Data data = new Data();

  protected synchronized boolean connected(NodeMessage msg) {
    UUID id = ProtocolUtils.convert(msg.getId());
    Preconditions.checkArgument(this.id.equals(id));
    if (msg.getStartTimestamp() < this.startTimestamp) {
      return false; // drop it
    }
    if (connected) {
      if (this.type != NodeMessage.Type.UNRECOGNIZED) {
        Preconditions.checkArgument(msg.getType() == this.type, "Type of node changed!");
      }
    }
    this.name = Optional.ofNullable(msg.getName().isEmpty() ? null : msg.getName().toLowerCase());
    this.type = msg.getType();
    synchronized (this.namespaces) {
      this.namespaces.clear();
      this.namespaces.addAll(msg.getNamespacesList());
    }
    synchronized (this.procedures) {
      Set<Procedure> procedures = msg.getProceduresList().stream().map(Procedure::new).collect(Collectors.toSet());
      this.procedures.clear();
      this.procedures.addAll(procedures);
    }
    this.data.set(msg);
    this.startTimestamp = msg.getStartTimestamp();
    this.connected = true;
    return true;
  }

  protected synchronized void disconnected() {
    this.connected = false;
  }

  public Set<String> getNamespaces() {
    synchronized (this.namespaces) {
      return ImmutableSet.copyOf(namespaces);
    }
  }

  public Set<Procedure> getProcedures() {
    synchronized (this.procedures) {
      return ImmutableSet.copyOf(procedures);
    }
  }

  public boolean hasNamespace(String namespace) {
    return this.namespaces.contains(namespace.toLowerCase());
  }

  public boolean hasNamespaceAny(Collection<String> namespaces) {
    for (String namespace : namespaces) {
      if (this.hasNamespace(namespace)) {
        return true;
      }
    }
    return false;
  }

  public boolean hasNamespace(Namespace namespace) {
    return hasNamespace(namespace.getName());
  }

  public boolean hasProcedure(Procedure info) {
    return procedures.contains(info);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Node[");
    if (name.isPresent()) {
      sb.append(name.get());
      sb.append(", ");
    }
    sb.append(this.getId().toString());
    sb.append("]");
    return sb.toString();
  }

  public boolean isPart(TransportProtocol.TargetMessage target) {
    return isPart(new Target(target));
  }

  public boolean isPart(Target target) {
    return target.contains(this);
  }

  // Data

  @Getter
  public class Data {
    private DataBukkitMessage bukkit = null;
    private DataBungeecordMessage bungeecord = null;
    private DataRouterMessage router = null;

    private void set(NodeMessage msg) {
      if (Node.this.type == Type.BUKKIT) {
        this.bukkit = msg.getDataBukkit();
      } else if (Node.this.type == Type.BUNGEECORD) {
        this.bungeecord = msg.getDataBungeecord();
      } else if (Node.this.type == Type.ROUTER) {
        this.router = msg.getDataRouter();
      }
    }

    public void put(NodeMessage.Builder builder) {
      if (builder.getType() == Type.BUKKIT && bukkit != null) {
        builder.setDataBukkit(bukkit);
      } else if (builder.getType() == Type.BUNGEECORD && bungeecord != null) {
        builder.setDataBungeecord(bungeecord);
      } else if (builder.getType() == Type.ROUTER && router != null) {
        builder.setDataRouter(router);
      }
    }

  }


  public NodeMessage toProtocol() {
    NodeMessage.Builder b = NodeMessage.newBuilder();
    b.setType(this.type);
    this.getData().put(b);
    b.setId(ProtocolUtils.convert(this.getId()));
    if (name.isPresent()) {
      b.setName(this.name.get());
    }
    b.setStartTimestamp(this.startTimestamp);
    b.addAllNamespaces(this.namespaces);
    this.procedures.stream().map(Procedure::toProtocol).forEach(b::addProcedures);
    return b.build();
  }


  // Home Node

  public static class HomeNode extends Node {

    @Getter
    private volatile boolean dirty = false;

    @Getter
    @Setter(AccessLevel.PACKAGE)
    @NonNull
    private AbstractNetwork network = null;

    public HomeNode(UUID id) {
      super(id);
      this.startTimestamp = System.currentTimeMillis();
    }

    public HomeNode(UUID id, Collection<String> namespaces) {
      this(id);
      Set<String> temp = Sets.newHashSet();
      namespaces.stream().filter(n -> !n.isEmpty()).forEach(n -> temp.add(n.toLowerCase()));
      this.namespaces.addAll(temp);
    }

    public void setType(NodeMessage.Type type) {
      Preconditions.checkNotNull(type);
      this.type = type;
    }

    public void addNamespace(String namespace) {
      Preconditions.checkArgument(!namespace.isEmpty());
      dirty |= this.namespaces.add(namespace.toLowerCase());
      publishChanges();
    }

    public void removeNamespace(String namespace) {
      Preconditions.checkArgument(!namespace.isEmpty());
      dirty |= this.namespaces.remove(namespace.toLowerCase());
      publishChanges();
    }

    public void setName(String name) {
      Optional<String> old = this.name;
      if (name == null || name.isEmpty()) {
        this.name = Optional.empty();
        if (old.isPresent()) {
          dirty = true;
        }
      } else {
        this.name = Optional.of(name.toLowerCase());
        if (!old.isPresent() || old.get().equalsIgnoreCase(name)) {
          dirty = true;
        }
      }
      publishChanges();
    }

    public void addRegisteredProcedure(CallableRegisteredProcedure<?, ?> procedure) {
      dirty |= this.procedures.add(procedure);
      publishChanges();
    }

    public void removeRegisteredProcedure(CallableRegisteredProcedure<?, ?> procedure) {
      dirty |= this.procedures.remove(procedure);
      publishChanges();
    }

    public void publishChanges() {
      if (!this.dirty) {
        return;
      }
      if (network.getHome() != this) {
        throw new IllegalStateException();
      }
      network.scheduleAsyncLater(() -> {
        try {
          publishChanges(network);
        } catch (Exception e) {
          network.getLogger().log(Level.SEVERE, "Failed to publish home node updates", e);
        }
      } , 100, TimeUnit.MICROSECONDS);
    }

    public void publishChanges(AbstractNetwork network) throws IOException {
      Preconditions.checkArgument(network.getHome() == this);
      if (!this.dirty) {
        return;
      }
      synchronized (this) {
        if (!this.dirty) {
          return;
        }
        this.dirty = false;
        network.sendHomeNodeUpdate();
      }
    }

    @Override
    public boolean connected(NodeMessage server) {
      throw new UnsupportedOperationException();
    }

    @Override
    protected synchronized void disconnected() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("HomeNode[");
      if (name.isPresent()) {
        sb.append(name.get());
        sb.append(", ");
      }
      sb.append(this.getId().toString());
      sb.append("]");
      return sb.toString();
    }

  }


}
