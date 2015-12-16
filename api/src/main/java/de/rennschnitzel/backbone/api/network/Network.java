package de.rennschnitzel.backbone.api.network;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.nustaq.serialization.FSTConfiguration;

import de.rennschnitzel.backbone.api.network.procedure.ProcedureCall;
import de.rennschnitzel.backbone.api.network.target.TargetOrBuilder;
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

  public abstract void handle(TransportProtocol.ContentMessage message) throws ProtocolException;

  public abstract void sendBytes(TargetOrBuilder target, String key, byte[] data);

  public abstract void sendObject(TargetOrBuilder target, Object object);

  public abstract Map<UUID, Server> getServers();

  public abstract Map<UUID, Server> getServers(TargetOrBuilder target);

  public abstract MessageEventBus getMessageEventBus();

  public abstract <T, R> void sendCall(ProcedureCall<T, R> call);


}
