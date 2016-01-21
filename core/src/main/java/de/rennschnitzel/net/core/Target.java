package de.rennschnitzel.net.core;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import de.rennschnitzel.net.ProtocolUtils;
import de.rennschnitzel.net.protocol.TransportProtocol;
import lombok.Getter;

@Getter
public class Target {
  
  private static Target TO_ALL = Builder.toAll().build();

  public static Target toAll() {
    return TO_ALL;
  }

  public static Target to(UUID nodeId) {
    return Builder.builder().include(nodeId).build();
  }

  public static Target to(Node node) {
    return to(node.getId());
  }


  public static Target to(Collection<Node> nodes) {
    return Builder.builder().includeAllNodes(nodes).build();
  }

  private final boolean toAll;
  private final Set<UUID> nodesInclude;
  private final Set<UUID> nodesExclude;
  private final Set<String> namespacesInclude;
  private final Set<String> namespacesExclude;

  private final TransportProtocol.TargetMessage protocolMessage;

  public Target(boolean toAll, Collection<UUID> nodesInclude, Collection<UUID> nodesExclude, Collection<String> namespacesInclude,
      Collection<String> namespacesExclude) {
    this.toAll = toAll;
    Set<UUID> incNodes = Sets.newHashSet(nodesInclude);
    incNodes.removeAll(nodesExclude);
    Set<String> incName = Sets.newHashSet(namespacesInclude);
    incName.removeAll(namespacesExclude);
    if (toAll) {
      incNodes.clear();
      incName.clear();
    }
    this.nodesInclude = ImmutableSet.copyOf(incNodes);
    this.nodesExclude = ImmutableSet.copyOf(nodesExclude);
    this.namespacesInclude = ImmutableSet.copyOf(incName);
    this.namespacesExclude = ImmutableSet.copyOf(namespacesExclude);
    this.protocolMessage = createProtocolMessage();
  }

  private TransportProtocol.TargetMessage createProtocolMessage() {
    TransportProtocol.TargetMessage.Builder b = TransportProtocol.TargetMessage.newBuilder();
    b.setToAll(toAll);
    b.addAllNodesInclude(ProtocolUtils.convert(this.nodesInclude));
    b.addAllNodesExclude(ProtocolUtils.convert(this.nodesExclude));
    b.addAllNamespacesInclude(this.namespacesInclude);
    b.addAllNamespacesExclude(this.namespacesExclude);
    return b.build();
  }

  public Target(TransportProtocol.TargetMessage t) {
    this(t.getToAll(), ProtocolUtils.convertProto(t.getNodesIncludeList()), ProtocolUtils.convertProto(t.getNodesExcludeList()),
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

  public boolean contains(Node node) {
    if (toAll) {
      if (nodesExclude.contains(node.getId())) {
        return false;
      }
      if (overlaps(this.namespacesExclude, node.getNamespaces())) {
        return false;
      }
      return true;
    }
    if (nodesInclude.contains(node.getId())) {
      return true;
    }
    if (overlaps(this.namespacesInclude, node.getNamespaces())) {
      return true;
    }
    return false;
  }

  public boolean isEmpty() {
    return this.toAll ? false : this.nodesInclude.isEmpty() && this.namespacesInclude.isEmpty();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Target[");
    if (toAll) {
      sb.append("toAll");
      if (!this.nodesExclude.isEmpty()) {
        sb.append(", excludedNodes:[");
        sb.append(this.nodesExclude.stream().map(UUID::toString).collect(Collectors.joining(", ")));
        sb.append("]");
      }
      if (!this.namespacesExclude.isEmpty()) {
        sb.append(", excludedNamespaces:[");
        sb.append(this.namespacesExclude.stream().collect(Collectors.joining(", ")));
        sb.append("]");
      }
    } else {
      if (!this.nodesInclude.isEmpty()) {
        sb.append("Nodes:[");
        sb.append(this.nodesInclude.stream().map(UUID::toString).collect(Collectors.joining(", ")));
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
    private final Set<UUID> nodesInclude = Sets.newHashSet();
    private final Set<UUID> nodesExclude = Sets.newHashSet();
    private final Set<String> namespacesInclude = Sets.newHashSet();
    private final Set<String> namespacesExclude = Sets.newHashSet();


    public Builder setToAll(boolean toAll) {
      this.toAll = toAll;
      return this;
    }


    public Builder include(Node node) {
      return this.include(node.getId());
    }

    public Builder include(UUID nodeId) {
      Preconditions.checkNotNull(nodeId);
      this.nodesInclude.add(nodeId);
      this.nodesExclude.remove(nodeId);
      return this;
    }

    public Builder include(UUID... nodes) {
      for (int i = 0; i < nodes.length; ++i) {
        include(nodes[i]);
      }
      return this;
    }

    public Builder includeAllNodes(Collection<Node> c) {
      c.forEach(this::include);
      return this;
    }

    public Builder exclude(Node server) {
      return this.exclude(server.getId());
    }

    public Builder exclude(UUID nodeId) {
      Preconditions.checkNotNull(nodeId);
      this.nodesExclude.add(nodeId);
      this.nodesInclude.remove(nodeId);
      return this;
    }

    public Builder exclude(UUID... nodes) {
      for (int i = 0; i < nodes.length; ++i) {
        exclude(nodes[i]);
      }
      return this;
    }

    public Builder excludeAllNodes(Collection<Node> c) {
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
      this.nodesInclude.removeAll(this.nodesExclude);
      this.namespacesInclude.removeAll(this.namespacesExclude);
      return new Target(this.toAll, this.nodesInclude, this.nodesExclude, this.namespacesInclude, this.namespacesExclude);
    }

  }


}
