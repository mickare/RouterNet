package de.rennschnitzel.backbone.net;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import de.rennschnitzel.backbone.ProtocolUtils;
import de.rennschnitzel.backbone.net.node.NetworkNode;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;
import lombok.Getter;

@Getter
public class Target {

  public static Target toAll() {
    return Builder.toAll().build();
  }

  public static Target to(UUID server) {
    return Builder.builder().include(server).build();
  }

  public static Target to(NetworkNode server) {
    return to(server.getId());
  }


  public static Target to(Collection<NetworkNode> servers) {
    return Builder.builder().includeAllServers(servers).build();
  }

  private final boolean toAll;
  private final Set<UUID> serversInclude;
  private final Set<UUID> serversExclude;
  private final Set<String> namespacesInclude;
  private final Set<String> namespacesExclude;

  private final TransportProtocol.TargetMessage protocolMessage;

  public Target(boolean toAll, Collection<UUID> serversInclude, Collection<UUID> serversExclude, Collection<String> namespacesInclude,
      Collection<String> namespacesExclude) {
    this.toAll = toAll;
    Set<UUID> incServ = Sets.newHashSet(serversInclude);
    incServ.removeAll(serversExclude);
    Set<String> incName = Sets.newHashSet(namespacesInclude);
    incName.removeAll(namespacesExclude);
    if (toAll) {
      incServ.clear();
      incName.clear();
    }
    this.serversInclude = ImmutableSet.copyOf(incServ);
    this.serversExclude = ImmutableSet.copyOf(serversExclude);
    this.namespacesInclude = ImmutableSet.copyOf(incName);
    this.namespacesExclude = ImmutableSet.copyOf(namespacesExclude);
    this.protocolMessage = createProtocolMessage();
  }

  private TransportProtocol.TargetMessage createProtocolMessage() {
    TransportProtocol.TargetMessage.Builder b = TransportProtocol.TargetMessage.newBuilder();
    b.setAll(toAll);
    b.addAllServersInclude(ProtocolUtils.convert(this.serversInclude));
    b.addAllServersExclude(ProtocolUtils.convert(this.serversExclude));
    b.addAllNamespacesInclude(this.namespacesInclude);
    b.addAllNamespacesExclude(this.namespacesExclude);
    return b.build();
  }

  public Target(TransportProtocol.TargetMessage t) {
    this(t.getAll(), ProtocolUtils.convertProto(t.getServersIncludeList()), ProtocolUtils.convertProto(t.getServersExcludeList()),
        t.getNamespacesIncludeList(), t.getNamespacesExcludeList());
  }

  public static boolean overlaps(Collection<?> c, Collection<?> b) {
    for (Object o : b) {
      if (c.contains(o)) {
        return true;
      }
    }
    return false;
  }

  public boolean contains(NetworkNode server) {
    if (toAll) {
      if (serversExclude.contains(server.getId())) {
        return false;
      }
      if (overlaps(this.namespacesExclude, server.getNamespaces())) {
        return false;
      }
      return true;
    }
    if (serversInclude.contains(server.getId())) {
      return true;
    }
    if (overlaps(this.namespacesInclude, server.getNamespaces())) {
      return true;
    }
    return false;
  }

  public boolean isEmpty() {
    return this.toAll ? false : this.serversInclude.isEmpty() && this.namespacesInclude.isEmpty();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Target[");
    if (toAll) {
      sb.append("toAll");
      if (!this.serversExclude.isEmpty()) {
        sb.append(", excludedServers:[");
        sb.append(this.serversExclude.stream().map(UUID::toString).collect(Collectors.joining(", ")));
        sb.append("]");
      }
      if (!this.namespacesExclude.isEmpty()) {
        sb.append(", excludedNamespaces:[");
        sb.append(this.namespacesExclude.stream().collect(Collectors.joining(", ")));
        sb.append("]");
      }
    } else {
      if (!this.serversInclude.isEmpty()) {
        sb.append("servers:[");
        sb.append(this.serversInclude.stream().map(UUID::toString).collect(Collectors.joining(", ")));
        sb.append("]");


        if (!this.namespacesInclude.isEmpty()) {
          sb.append(", ");
        }
      }
      if (!this.namespacesInclude.isEmpty()) {
        sb.append("namespaces:[");
        sb.append(this.namespacesInclude.stream().collect(Collectors.joining(", ")));
        sb.append("]");
      }
    }
    sb.append("]");
    return sb.toString();
  }

  // *********************************************************
  // Builder

  public static class Builder {

    public static Builder toAll() {
      return new Builder().setToAll(true);
    }

    public static Builder builder() {
      return new Builder();
    }

    private boolean toAll = false;
    private final Set<UUID> serversInclude = Sets.newHashSet();
    private final Set<UUID> serversExclude = Sets.newHashSet();
    private final Set<String> namespacesInclude = Sets.newHashSet();
    private final Set<String> namespacesExclude = Sets.newHashSet();


    public Builder setToAll(boolean toAll) {
      this.toAll = toAll;
      return this;
    }


    public Builder include(NetworkNode server) {
      return this.include(server.getId());
    }

    public Builder include(UUID server) {
      Preconditions.checkNotNull(server);
      this.serversInclude.add(server);
      this.serversExclude.remove(server);
      return this;
    }

    public Builder include(UUID... servers) {
      for (int i = 0; i < servers.length; ++i) {
        include(servers[i]);
      }
      return this;
    }

    public Builder includeAllServers(Collection<NetworkNode> c) {
      c.forEach(this::include);
      return this;
    }

    public Builder exclude(NetworkNode server) {
      return this.exclude(server.getId());
    }

    public Builder exclude(UUID server) {
      Preconditions.checkNotNull(server);
      this.serversExclude.add(server);
      this.serversInclude.remove(server);
      return this;
    }

    public Builder exclude(UUID... servers) {
      for (int i = 0; i < servers.length; ++i) {
        exclude(servers[i]);
      }
      return this;
    }

    public Builder excludeAllServers(Collection<NetworkNode> c) {
      c.forEach(this::exclude);
      return this;
    }

    public Builder include(String namespace) {
      String n = namespace.toLowerCase();
      this.namespacesInclude.add(n);
      this.namespacesExclude.remove(n);
      return this;
    }

    public Builder include(String... namespaces) {
      for (int i = 0; i < namespaces.length; ++i) {
        include(namespaces[i]);
      }
      return this;
    }

    public Builder includeAllNamespaces(Collection<String> c) {
      c.forEach(this::include);
      return this;
    }

    public Builder exclude(String namespace) {
      String n = namespace.toLowerCase();
      this.namespacesExclude.add(n);
      this.namespacesInclude.remove(n);
      return this;
    }

    public Builder exclude(String... namespaces) {
      for (int i = 0; i < namespaces.length; ++i) {
        exclude(namespaces[i]);
      }
      return this;
    }

    public Builder excludeAllNamespaces(Collection<String> c) {
      c.forEach(this::exclude);
      return this;
    }

    public Target build() {
      this.serversInclude.removeAll(this.serversExclude);
      this.namespacesInclude.removeAll(this.namespacesExclude);
      return new Target(this.toAll, this.serversInclude, this.serversExclude, this.namespacesInclude, this.namespacesExclude);
    }

  }


}
