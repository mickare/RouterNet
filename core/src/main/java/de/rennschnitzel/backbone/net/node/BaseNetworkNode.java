package de.rennschnitzel.backbone.net.node;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.ProtocolUtils;
import de.rennschnitzel.backbone.net.Target;
import de.rennschnitzel.backbone.net.procedure.ProcedureInformation;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.ServerMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.TargetMessage;
import lombok.Getter;

public class BaseNetworkNode implements NetworkNode {

  @Getter
  private final UUID id;
  @Getter
  protected Optional<String> name = Optional.empty();
  protected final Set<String> namespaces = new CopyOnWriteArraySet<>();
  @Getter
  private long timestamp = System.currentTimeMillis();
  protected final Set<ProcedureInformation> procedures = new CopyOnWriteArraySet<>();
  @Getter
  protected ServerMessage.Type type = ServerMessage.Type.UNDEFINED;

  public BaseNetworkNode(UUID id) {
    Preconditions.checkNotNull(id);
    this.id = id;
  }

  @Override
  public Set<String> getNamespaces() {
    return Collections.unmodifiableSet(this.namespaces);
  }

  @Override
  public boolean hasNamespace(String namespace) {
    return this.namespaces.contains(namespace.toLowerCase());
  }

  @Override
  public boolean hasNamespace(Collection<String> namespaces) {
    for (String namespace : namespaces) {
      if (this.hasNamespace(namespace)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void update(ServerMessage server) {
    Preconditions.checkArgument(this.id.equals(ProtocolUtils.convert(server.getId())));
    this.name = Optional.ofNullable(server.getName().isEmpty() ? null : server.getName().toLowerCase());
    this.type = server.getType();
    this.namespaces.retainAll(server.getNamespacesList());
    this.namespaces.addAll(server.getNamespacesList());
    this.timestamp = server.getTimestamp();
    List<ProcedureInformation> procedures = server.getProceduresList().stream().map(ProcedureInformation::new).collect(Collectors.toList());
    this.procedures.retainAll(procedures);
    this.procedures.addAll(procedures);

  }

  @Override
  public boolean isPart(TargetMessage target) {
    if (target.getAll()) {
      if (ProtocolUtils.convertProto(target.getServersExcludeList()).contains(this.id)) {
        return false;
      }
      if (Target.overlaps(target.getNamespacesExcludeList(), this.namespaces)) {
        return false;
      }
      return true;
    }
    if (ProtocolUtils.convertProto(target.getServersIncludeList()).contains(this.id)) {
      return true;
    }
    if (Target.overlaps(target.getNamespacesIncludeList(), this.namespaces)) {
      return true;
    }
    return false;
  }

  @Override
  public boolean hasProcedure(ProcedureInformation info) {
    return procedures.contains(info);
  }

  @Override
  public ServerMessage toProtocol() {
    ServerMessage.Builder b = ServerMessage.newBuilder();
    b.setId(this.getIdProto());
    if (name.isPresent()) {
      b.setName(this.name.get());
    }
    b.setTimestamp(this.timestamp);
    b.addAllNamespaces(this.namespaces);
    this.procedures.stream().map(p -> p)
    b.addAllProcedures(this.procedures);

    return b.build();
  }


}
