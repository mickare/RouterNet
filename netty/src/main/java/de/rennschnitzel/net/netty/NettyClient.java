package de.rennschnitzel.net.netty;

import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class NettyClient {

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
  private final Logger logger;
  private EventLoopGroup group = null;
  private ChannelFuture clientFuture = null;
  private Channel client = null;

  protected abstract MainHandler<?> newMainHandler();

  public boolean isConnected() {
    Channel c = client;
    return c != null ? c.isActive() : false;
  }

  public synchronized Future<NettyClient> connect() {
    Preconditions.checkState(this.state == State.NEW);
    this.state = State.CONNECTING;


    try {
      group = PipelineUtils.newEventLoopGroup(0, new ThreadFactoryBuilder()
          .setNameFormat("NetClient " + name + " IO Thread #%1$d").build());

      Bootstrap b = new Bootstrap();
      b.group(group) //
          .channel(NioSocketChannel.class)//
          // .option(ChannelOption.)
          .handler(new BaseChannelInitializer(this::newMainHandler));
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
        group.shutdownGracefully();
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
    if (group != null) {
      group.shutdownGracefully();
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
      group.shutdownGracefully();

      state = State.DISCONNECTED;

    } catch (Exception e) {
      fail(e);
    }

  }

}
