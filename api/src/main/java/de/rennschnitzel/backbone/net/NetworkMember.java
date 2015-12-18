package de.rennschnitzel.backbone.net;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.ProtocolUtils;
import de.rennschnitzel.backbone.net.procedure.ProcedureInformation;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.TargetMessage;
import lombok.Getter;

public class NetworkMember {

  @Getter
  private final UUID id;
  @Getter
  private Optional<String> name = Optional.empty();
  private final Set<String> namespaces = new CopyOnWriteArraySet<>();

  public NetworkMember(UUID id) {
    Preconditions.checkNotNull(id);
    this.id = id;
  }

  public Set<String> getNamespaces() {
    return Collections.unmodifiableSet(this.namespaces);
  }

  public boolean hasNamespace(String namespace) {
    return this.namespaces.contains(namespace.toLowerCase());
  }

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

  public boolean hasProcedure(ProcedureInformation info) {
    // TODO Auto-generated method stub
    return false;
  }


}
