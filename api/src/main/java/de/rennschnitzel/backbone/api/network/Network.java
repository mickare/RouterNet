package de.rennschnitzel.backbone.api.network;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.nustaq.serialization.FSTConfiguration;

import de.rennschnitzel.backbone.api.network.procedure.ProcedureCall;
import de.rennschnitzel.backbone.api.network.target.TargetOrBuilder;
import de.rennschnitzel.backbone.net.NetworkMember;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;
import de.rennschnitzel.backbone.netty.exception.ProtocolException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

public abstract class Network {

  public static FSTConfiguration FST = FSTConfiguration.createDefaultConfiguration();

  @Getter
  @Setter(AccessLevel.PROTECTED)
  private static Network instance = null;

  public abstract UUID getID();

  public abstract Set<String> getNamespaces();

  public abstract Map<UUID, NetworkMember> getServers();

  public abstract Map<UUID, NetworkMember> getServersOfTarget(TargetOrBuilder target);

  public abstract Map<UUID, NetworkMember> getServersOfNamespace(String namespace, String... namespaces);

  public abstract <T, R> void sendCall(ProcedureCall<T, R> call);


}
