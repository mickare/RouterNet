package de.rennschnitzel.backbone.net;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.ImmutableSet;

import de.rennschnitzel.backbone.ProtocolUtils;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;
import lombok.Getter;

@Getter
public class Target {

  public static Target toAll() {
    return new Target(true, Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
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
    this.serversInclude = ImmutableSet.copyOf(serversInclude);
    this.serversExclude = ImmutableSet.copyOf(serversExclude);
    this.namespacesInclude = ImmutableSet.copyOf(namespacesInclude);
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

  public boolean contains(NetworkMember server) {
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

}
