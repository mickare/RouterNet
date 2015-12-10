package de.rennschnitzel.backbone.api.network;

import java.util.UUID;

import de.rennschnitzel.backbone.api.network.message.ByteMessage;
import de.rennschnitzel.backbone.api.network.message.ObjectMessage;
import de.rennschnitzel.backbone.api.network.procedure.ProcedureCall;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.CloseMessage;
import io.netty.channel.ChannelFuture;

public interface Connection {

  UUID getRouterUUID();

  UUID getClientUUID();

  boolean isOpen();

  boolean isActive();

  ChannelFuture close();

  ChannelFuture close(CloseMessage packet);

  // void send(ErrorMessage packet) throws NotConnectedException;

  // void send(ContentMessage packet) throws NotConnectedException;

  void send(ByteMessage message);

  void send(ObjectMessage message);

  <T, R> void sendCall(ProcedureCall<T, R> call);

}
