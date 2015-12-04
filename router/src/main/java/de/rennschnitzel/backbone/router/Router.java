package de.rennschnitzel.backbone.router;

import java.util.logging.Logger;

import de.rennschnitzel.backbone.api.network.RouterInfo;
import de.rennschnitzel.backbone.net.protocol.ComponentUUID;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol;
import de.rennschnitzel.backbone.netty.BackboneChannelInitializer;
import de.rennschnitzel.backbone.service.AbstractDirectService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Router extends AbstractDirectService {


  private final RouterInfo router;

  private EventLoopGroup bossGroup, workerGroup;

  private final Logger logger;

  @Override
  protected void startUp() throws Exception {


    bossGroup = new NioEventLoopGroup(1);
    workerGroup = new NioEventLoopGroup();



    ServerBootstrap b = new ServerBootstrap();
    b.group(bossGroup, workerGroup);
    b.channel(NioServerSocketChannel.class);
    b.handler(new LoggingHandler(LogLevel.INFO));
    b.childHandler(new ChannelInitializer<SocketChannel>() {
      @Override
      protected void initChannel(SocketChannel ch) throws Exception {
        RouterHandshakeHandler handshake = new RouterHandshakeHandler();
        new BackboneChannelInitializer(logger, handshake).initChannel(ch);

      }
    });

    b.bind(router.getPort());

  }

  @Override
  protected void shutDown() throws Exception {
    // TODO Auto-generated method stub

  }

  public NetworkProtocol.Router toProtocol() {
    NetworkProtocol.Router.Builder r = NetworkProtocol.Router.newBuilder();
    r.setId(ComponentUUID.UUID.newBuilder()
        .setMostSignificantBits(router.getId().getMostSignificantBits())
        .setLeastSignificantBits(router.getId().getLeastSignificantBits()));
    r.setName(router.getName());
    r.setTimestamp(System.currentTimeMillis());
    r.setAddress(
        NetworkProtocol.Address.newBuilder().setHost(router.getHost()).setPort(router.getPort()));
    return null;
  }

}
