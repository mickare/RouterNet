package de.rennschnitzel.net.netty;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.rennschnitzel.net.core.AbstractNetwork;
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
public class NettyServer extends AbstractService {

  @Getter
  @NonNull
  private final String name;
  @Getter
  @NonNull
  private HostAndPort address;
  @Getter
  @NonNull
  private final Logger logger = AbstractNetwork.getInstance().getLogger();
  private EventLoopGroup eventLoop = null;
  private ChannelFuture channelFuture = null;
  private Channel channel = null;

  @NonNull
  private final ChannelInitializer<SocketChannel> channelInitializer;


  @Override
  protected void doStart() {
    try {
      startUp();
    } catch (Throwable t) {
      notifyFailed(t);
    }
  }

  @Override
  protected void doStop() {
    try {
      shutDown();
      notifyStopped();
    } catch (Throwable t) {
      notifyFailed(t);
    }
  }

  protected void startUp() throws Exception {
    eventLoop = PipelineUtils.newEventLoopGroup(0,
        new ThreadFactoryBuilder().setNameFormat("NetServer " + name + " IO Thread #%1$d").build());

    ServerBootstrap b = new ServerBootstrap();
    b.channel(PipelineUtils.getServerChannelClass());
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
          notifyStarted();
        } else {
          shutDown();
          logger.log(Level.INFO, name + " failed to listen on " + address.toString(),
              future.cause());
          notifyFailed(future.cause());
        }
      }
    });

  }

  protected void shutDown() throws Exception {
    try {
      if (channelFuture != null) {
        channelFuture.cancel(true);
      }
      if (channel != null) {
        ChannelFuture closeFuture = channel.close();
        closeFuture.addListener(new ChannelFutureListener() {
          @Override
          public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
              logger.log(Level.INFO, name + " closed {0}", address.toString());
            } else {
              logger.log(Level.INFO, name + " failed to close " + address.toString(),
                  future.cause());
              notifyFailed(future.cause());
            }
          }
        });
        closeFuture.sync();
      }
    } finally {
      if (eventLoop != null) {
        eventLoop.shutdownGracefully().await(500, TimeUnit.MILLISECONDS);
      }
    }
  }

}
