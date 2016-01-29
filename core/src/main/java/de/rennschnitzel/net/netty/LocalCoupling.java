package de.rennschnitzel.net.netty;

import java.nio.channels.ClosedChannelException;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.util.FutureUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.util.concurrent.Promise;
import lombok.Getter;
import lombok.Setter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LocalCoupling implements AutoCloseable {

  public static enum State {
    NEW, STARTING, ACTIVE, CLOSED, FAILED;
  };

  private static final AtomicInteger COUNTER = new AtomicInteger(0);

  private final LocalAddress addr = new LocalAddress("localconnection." + COUNTER.getAndIncrement());

  @NonNull
  private final ChannelInitializer<LocalChannel> serverInit;
  @NonNull
  private final ChannelInitializer<LocalChannel> clientInit;

  @Getter
  private volatile State state = State.NEW;

  @Setter
  @Getter
  private boolean shutdownGroup = true;
  private EventLoopGroup group = null;
  private Channel serverChannel;
  private Channel clientChannel;

  public LocalCoupling(ChannelInitializer<LocalChannel> serverInit, ChannelInitializer<LocalChannel> clientInit, EventLoopGroup group) {
    this(serverInit, clientInit);
    Preconditions.checkArgument(!group.isShutdown());
    this.group = group;
    this.shutdownGroup = false;
  }

  private final Promise<Void> promise = FutureUtils.newPromise();

  public AutoCloseable open() {
    Preconditions.checkState(state == State.NEW);
    this.state = State.STARTING;

    try {
      if (group == null) {
        group = new DefaultEventLoopGroup();
      }

      Bootstrap cb = new Bootstrap();
      cb.remoteAddress(addr);
      cb.group(group).channel(LocalChannel.class).handler(clientInit);

      ServerBootstrap sb = new ServerBootstrap();
      sb.group(group).channel(LocalServerChannel.class).childHandler(serverInit);

      // Start server
      sb.bind(addr).addListener((ChannelFutureListener) sf -> {
        if (sf.isSuccess()) {
          serverChannel = sf.channel();
          cb.connect(addr).addListener((ChannelFutureListener) cf -> {
            if (cf.isSuccess()) {
              clientChannel = cf.channel();
              state = State.ACTIVE;
              promise.trySuccess(null);
            } else {
              fail(cf.cause());
            }
          });
        } else {
          fail(sf.cause());
        }
      });

    } catch (Exception e) {
      fail(e);
    }
    return this;
  }

  public void awaitRunning() throws InterruptedException {
    promise.await();
  }

  public void awaitRunning(long timeoutMillis) throws InterruptedException {
    promise.await(timeoutMillis);
  }

  private void fail(Throwable cause) {
    this.promise.tryFailure(cause);
    close();
    this.state = State.FAILED;
  }

  @Override
  public void close() {
    if (state == State.CLOSED) {
      return;
    }
    state = State.CLOSED;
    this.promise.tryFailure(new ClosedChannelException());
    if (serverChannel != null) {
      serverChannel.close().awaitUninterruptibly();
    }
    if (clientChannel != null) {
      clientChannel.close();
    }
    if (shutdownGroup) {
      group.shutdownGracefully();
    }
  }


}
