package de.rennschnitzel.backbone.router;

import java.util.UUID;

import de.rennschnitzel.backbone.api.Connection;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ErrorMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.Message;
import de.rennschnitzel.backbone.netty.exception.NotConnectedException;
import io.netty.channel.ChannelFuture;

public class ClientConnection implements Connection {

  public UUID getRouterUUID() {
    // TODO Auto-generated method stub
    return null;
  }

  public UUID getClientUUID() {
    // TODO Auto-generated method stub
    return null;
  }

  public boolean isOpen() {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean isActive() {
    // TODO Auto-generated method stub
    return false;
  }

  public ChannelFuture close() {
    // TODO Auto-generated method stub
    return null;
  }

  public ChannelFuture close(CloseMessage packet) {
    // TODO Auto-generated method stub
    return null;
  }

  public void send(ErrorMessage packet) throws NotConnectedException {
    // TODO Auto-generated method stub
    
  }

  public void send(Message packet) throws NotConnectedException {
    // TODO Auto-generated method stub
    
  }

}
