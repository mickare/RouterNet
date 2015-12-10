package de.rennschnitzel.backbone.api.network;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import de.rennschnitzel.backbone.api.network.target.TargetOrBuilder;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;
import de.rennschnitzel.backbone.netty.exception.ProtocolException;

public interface NetworkInterface {

  UUID getID();

  Set<String> getNamespaces();

  void handle(TransportProtocol.ContentMessage message) throws ProtocolException;

  void sendBytes(TargetOrBuilder target, String key, byte[] data);

  void sendObject(TargetOrBuilder target, Object object);

  Map<UUID, Server> getServers();

  Map<UUID, Server> getServers(TargetOrBuilder target);

  Connection getConnection();

}
