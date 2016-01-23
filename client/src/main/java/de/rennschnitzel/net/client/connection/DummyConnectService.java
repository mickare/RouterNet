package de.rennschnitzel.net.client.connection;

import de.rennschnitzel.net.NetClient;
import de.rennschnitzel.net.Network;
import de.rennschnitzel.net.core.packet.PacketHandler;
import de.rennschnitzel.net.dummy.DummyConnection;
import de.rennschnitzel.net.dummy.DummyLoginClientHandler;
import de.rennschnitzel.net.netty.NettyConnection;
import io.netty.util.concurrent.Future;

public class DummyConnectService
    extends AbstractConnectService<DummyLoginClientHandler, DummyConnection> {

  public DummyConnectService(NetClient client) {
    super(client);
    // TODO Auto-generated constructor stub
  }

  @Override
  protected Future<?> doConnect(L loginHandler,
      PacketHandler<NettyConnection<Network>> packetHandler) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected L createLoginHandler() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected Future<?> doConnect(DummyClientLoginHandler loginHandler,
      PacketHandler<DummyConnection> packetHandler) {
    // TODO Auto-generated method stub
    return null;
  }

}
