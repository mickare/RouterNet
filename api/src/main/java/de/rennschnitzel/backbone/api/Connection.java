package de.rennschnitzel.backbone.api;

import java.util.UUID;

import de.rennschnitzel.backbone.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ErrorMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.Message;
import de.rennschnitzel.backbone.netty.exception.NotConnectedException;
import io.netty.channel.ChannelFuture;

public interface Connection {
  
  UUID getRouterUUID();
  
  UUID getClientUUID();
  
  boolean isOpen();
  
  boolean isActive();
  
  ChannelFuture close();

  ChannelFuture close(CloseMessage packet);

  void send(ErrorMessage packet) throws NotConnectedException;

  void send(Message packet) throws NotConnectedException;

}
