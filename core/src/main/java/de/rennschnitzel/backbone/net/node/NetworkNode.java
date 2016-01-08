package de.rennschnitzel.backbone.net.node;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import de.rennschnitzel.backbone.ProtocolUtils;
import de.rennschnitzel.backbone.net.procedure.ProcedureInformation;
import de.rennschnitzel.backbone.net.protocol.ComponentUUID;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.ServerMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.TargetMessage;

public interface NetworkNode {

  UUID getId();

  default ComponentUUID.UUID getIdProto() {
    return ProtocolUtils.convert(getId());
  }

  NetworkProtocol.ServerMessage.Type getType();
  
  Optional<String> getName();

  Set<String> getNamespaces();

  long getTimestamp();

  boolean hasNamespace(String namespace);

  void update(ServerMessage server);

  boolean isPart(TargetMessage target);

  boolean hasProcedure(ProcedureInformation info);

  boolean hasNamespace(Collection<String> namespaces);

  ServerMessage toProtocol();


}
