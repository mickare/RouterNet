package de.rennschnitzel.net.client.connection;

import javax.net.ssl.SSLException;

import com.google.common.net.HostAndPort;

import de.rennschnitzel.net.NetClient;
import de.rennschnitzel.net.Network;
import de.rennschnitzel.net.netty.MainChannelInitializer;
import de.rennschnitzel.net.netty.MainHandler;
import de.rennschnitzel.net.netty.NettyClient;
import de.rennschnitzel.net.netty.NettyConnection;
import de.rennschnitzel.net.netty.PipelineUtils;
import de.rennschnitzel.net.netty.login.NettyLoginClientHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.Future;

public class NettyConnectService
    extends AbstractConnectService<NettyLoginClientHandler, NettyConnection<Network>> {

  private int attempt = 0;

  private final SslContext sslCtx;

  public NettyConnectService(NetClient client) throws SSLException {
    super(client);
    this.sslCtx = PipelineUtils.sslContextForClient();
  }

  @Override
  protected Future<?> doConnect(final NettyLoginClientHandler loginHandler) {
    attempt++;

    MainChannelInitializer init =
        new MainChannelInitializer(() -> new MainHandler<Network>(getClient().getNetwork(),
            loginHandler, NettyConnectService.this.createPacketHandler()), sslCtx);

    final HostAndPort address = getClient().getRouterAddress();

    NettyClient connector = new NettyClient("NettyConnect #" + attempt, address, init);

    Future<?> future = connector.connect();
    future.addListener(f -> {
      if (f.isSuccess()) {
        getLogger().info("Connecting to " + address + "...");
      }
    });
    return future;
  }

  @Override
  protected NettyLoginClientHandler createLoginHandler() {
    return new NettyLoginClientHandler(getClient().getNetwork(), getClient().getAuthentication());
  }

}
