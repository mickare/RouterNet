package de.rennschnitzel.backbone.router;

import de.rennschnitzel.backbone.net.Connection;
import de.rennschnitzel.backbone.net.Network;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.Packet;
import io.netty.channel.ChannelHandlerContext;

public class ClientConnection extends Connection {

  private final ChannelHandlerContext ctx;
  
  public ClientConnection(Network network) {
    super(network);
  }

  @Override
  public void send(Packet packet) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public boolean isClosed() {
    // TODO Auto-generated method stub
    return false;
  }


}
