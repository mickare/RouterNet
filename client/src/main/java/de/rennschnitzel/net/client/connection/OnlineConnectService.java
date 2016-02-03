package de.rennschnitzel.net.client.connection;

import javax.net.ssl.SSLException;

import de.rennschnitzel.net.NetClient;
import de.rennschnitzel.net.client.OnlineConnectClient;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.login.ClientLoginEngine;
import de.rennschnitzel.net.core.packet.BasePacketHandler;
import de.rennschnitzel.net.netty.ConnectionHandler;
import de.rennschnitzel.net.netty.LoginHandler;
import de.rennschnitzel.net.netty.PipelineUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.Promise;

public class OnlineConnectService extends AbstractConnectService {

  private final SslContext sslCtx;

  public OnlineConnectService(NetClient client) throws SSLException {
    super(client);
    this.sslCtx = PipelineUtils.sslContextForClient();
  }

  @Override
  protected OnlineConnectClient newConnectClient(Promise<Connection> future) {

    ClientLoginEngine engine =
        new ClientLoginEngine(getClient().getNetwork(), getClient().getAuthentication());

    ChannelInitializer<Channel> init = PipelineUtils.baseInitAnd(ch -> {
      ch.pipeline().addLast(new LoginHandler(engine, future));
      ch.pipeline()
          .addLast(new ConnectionHandler(getClient().getNetwork(), BasePacketHandler.DEFAULT));
    });

    return new OnlineConnectClient(getClient().getRouterAddress(), init, this.getGroup());
  }


}
