package de.rennschnitzel.backbone.net.node;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import de.rennschnitzel.backbone.net.procedure.ProcedureInformation;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.ServerMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.TargetMessage;

public interface NetworkNode {

  UUID getId();

  Optional<String> getName();

  Set<String> getNamespaces();

  long getTimestamp();

  boolean hasNamespace(String namespace);

  void update(ServerMessage server);

  boolean isPart(TargetMessage target);

  boolean hasProcedure(ProcedureInformation info);

  boolean hasNamespace(Collection<String> namespaces);


}
