package de.rennschnitzel.net.netty;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.rennschnitzel.net.service.AbstractDirectService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class NettyServer extends AbstractDirectService {

  @Getter
  @NonNull
  private final String name;
  @Getter
  @NonNull
  private HostAndPort address;
  @Getter
  @NonNull
  private final Logger logger;
  private EventLoopGroup eventLoop = null;
  private ChannelFuture channelFuture = null;
  private Channel channel = null;

  @NonNull
  private final ChannelInitializer<SocketChannel> channelInitializer;

  @Override
  protected void startUp() throws Exception {
    eventLoop = PipelineUtils.newEventLoopGroup(0,
        new ThreadFactoryBuilder().setNameFormat("NetServer " + name + " IO Thread #%1$d").build());

    ServerBootstrap b = new ServerBootstrap();
    b.group(eventLoop);
    b.option(ChannelOption.SO_REUSEADDR, true);
    b.childHandler(channelInitializer);
    channelFuture = b.bind(address.getHostText(), address.getPort());

    channelFuture.addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        if (future.isSuccess()) {
          channel = future.channel();
          logger.log(Level.INFO, name + " listening on {0}", address.toString());
        } else {
          fail(future.cause());
          logger.log(Level.INFO, name + " failed to listen on " + address.toString(),
              future.cause());
        }
      }
    });

  }

  @Override
  protected void shutDown() throws Exception {
    stopNetty();
  }

  private void fail(Throwable cause) throws InterruptedException {
    notifyFailed(cause);
    stopNetty();
  }

  private void stopNetty() throws InterruptedException {
    if (channelFuture != null) {
      channelFuture.cancel(true);
    }
    if (channel != null) {
      channel.close().sync();
    }
    if (eventLoop != null) {
      eventLoop.shutdownGracefully();
    }
  }



}
