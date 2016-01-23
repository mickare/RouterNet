package de.rennschnitzel.net.client.connection;

import de.rennschnitzel.net.NetClient;
import de.rennschnitzel.net.Network;
import de.rennschnitzel.net.netty.MainChannelInitializer;
import de.rennschnitzel.net.netty.MainHandler;
import de.rennschnitzel.net.netty.NettyClient;
import de.rennschnitzel.net.netty.NettyConnection;
import de.rennschnitzel.net.netty.login.NettyLoginClientHandler;
import io.netty.util.concurrent.Future;

public class NettyConnectService
    extends AbstractConnectService<NettyLoginClientHandler, NettyConnection<Network>> {

  private int attempt = 0;

  public NettyConnectService(NetClient client) {
    super(client);
  }

  @Override
  protected Future<?> doConnect(final NettyLoginClientHandler loginHandler) {
    attempt++;

    MainChannelInitializer init =
        new MainChannelInitializer(() -> new MainHandler<Network>(getClient().getNetwork(),
            loginHandler, NettyConnectService.this.createPacketHandler()));

    NettyClient connector =
        new NettyClient("NettyConnect #" + attempt, getClient().getRouterAddress(), init);

    return connector.connect();
  }

  @Override
  protected NettyLoginClientHandler createLoginHandler() {
    return new NettyLoginClientHandler(getClient().getNetwork(), getClient().getAuthentication());
  }

}
