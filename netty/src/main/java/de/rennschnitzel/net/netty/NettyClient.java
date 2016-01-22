package de.rennschnitzel.net.netty;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.rennschnitzel.net.core.AbstractNetwork;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NettyClient {

  public static enum State {
    NEW, CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED, FAILED;
  }

  @Getter
  private volatile State state = State.NEW;
  @Getter
  private Throwable failureCause = null;
  @Getter
  @NonNull
  private final String name;
  @Getter
  @NonNull
  private final HostAndPort address;
  @Getter
  @NonNull
  private final Logger logger = AbstractNetwork.getInstance().getLogger();
  private EventLoopGroup eventLoop = null;
  private ChannelFuture clientFuture = null;
  private Channel client = null;

  @NonNull
  private final ChannelInitializer<SocketChannel> channelInitializer;

  public boolean isConnected() {
    Channel c = client;
    return c != null ? c.isActive() : false;
  }


  public boolean hasFailed() {
    return state == State.FAILED;
  }


  public synchronized Future<NettyClient> connect() {
    Preconditions.checkState(this.state == State.NEW);
    this.state = State.CONNECTING;


    try {
      eventLoop = PipelineUtils.newEventLoopGroup(0, new ThreadFactoryBuilder()
          .setNameFormat("NetClient " + name + " IO Thread #%1$d").build());

      Bootstrap b = new Bootstrap();
      b.group(eventLoop) //
          .channel(NioSocketChannel.class)//
          // .option(ChannelOption.)
          .handler(channelInitializer);
      clientFuture = b.connect(address.getHostText(), address.getPort());

      clientFuture.addListener(new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
          if (future.isSuccess()) {
            state = State.CONNECTED;
            client = future.channel();
            addCloseListener(future.channel());
            logger.log(Level.INFO, "Connected to {0}", address);
          } else {
            fail(future.cause());
            logger.log(Level.WARNING, "Could not connect to to " + address.toString(),
                future.cause());
          }
        }
      });

      return Futures.lazyTransform(clientFuture, v -> this);

    } catch (Exception e) {
      fail(e);
      return Futures.immediateFailedFuture(e);
    }

  }

  private void addCloseListener(Channel channel) {
    channel.closeFuture().addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        eventLoop.shutdownGracefully();
        state = State.DISCONNECTED;
      }
    });
  }

  private void fail(Throwable t) {
    state = State.FAILED;
    failureCause = t;
    if (client != null) {
      client.close();
    }
    if (eventLoop != null) {
      try {
        eventLoop.shutdownGracefully().await(500, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
      };
    }
  }

  public synchronized void disconnect() {
    if (state == State.NEW) {
      state = State.DISCONNECTED;
      return;
    }
    if (state == State.DISCONNECTED || this.state == State.FAILED) {
      return;
    }

    state = State.DISCONNECTING;

    try {

      if (clientFuture != null) {
        clientFuture.cancel(true);
      }
      if (client != null) {
        client.close().syncUninterruptibly();
      }
      eventLoop.shutdownGracefully().await(500, TimeUnit.MILLISECONDS);;

      state = State.DISCONNECTED;

    } catch (Exception e) {
      fail(e);
    }

  }

}
