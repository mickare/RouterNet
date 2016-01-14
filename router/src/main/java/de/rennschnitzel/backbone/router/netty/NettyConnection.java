package de.rennschnitzel.backbone.router.netty;

import de.rennschnitzel.backbone.net.Connection;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.Packet;
import de.rennschnitzel.backbone.netty.ConnectionBasePacketAdapter;
import lombok.Getter;

public class NettyConnection extends Connection {


  @Getter
  private final RouterPacketHandler packetHandler = new RouterPacketHandler();
  @Getter
  private final ConnectionBasePacketAdapter<NettyConnection> protocolHandler;

  public NettyConnection() {
    protocolHandler = new ConnectionBasePacketAdapter<>(this, packetHandler);

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
