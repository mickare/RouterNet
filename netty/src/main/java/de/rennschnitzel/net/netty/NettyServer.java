package de.rennschnitzel.net.netty;

import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.rennschnitzel.net.service.AbstractDirectService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

public abstract class NettyServer extends AbstractDirectService {

  private HostAndPort address;
  private EventLoopGroup eventLoops;
  private Channel serverListener;

  public NettyServer(HostAndPort address) {
    this.address = address;
  }

  protected abstract MainHandler<?> newMainHandler();

  @Override
  protected void startUp() throws Exception {
    eventLoops = new NioEventLoopGroup(0,
        new ThreadFactoryBuilder().setNameFormat("NettyServer IO Thread #%1$d").build());

    ServerBootstrap b = new ServerBootstrap();
    b.group(eventLoops);
    b.option(ChannelOption.SO_REUSEADDR, true);
    b.childHandler(new BaseChannelInitializer(this::newMainHandler));
    this.serverListener = b.bind(address.getHostText(), address.getPort()).sync().channel();


  }

  @Override
  protected void shutDown() throws Exception {
    serverListener.close().syncUninterruptibly();
    eventLoops.shutdownGracefully();
  }


}
