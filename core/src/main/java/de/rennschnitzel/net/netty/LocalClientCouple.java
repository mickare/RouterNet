package de.rennschnitzel.net.netty;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Preconditions;

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
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;


public @RequiredArgsConstructor class LocalClientCouple implements AutoCloseable {

  private static final AtomicInteger COUNTER = new AtomicInteger(0);

  public static enum State {
    NEW, STARTING, ACTIVE, CLOSED, FAILED;
  };

  private @Getter final LocalAddress addr = new LocalAddress("localconnection." + COUNTER.getAndIncrement());
  private @NonNull final ChannelInitializer<LocalChannel> serverInit;
  private @NonNull final ChannelInitializer<LocalChannel> clientInit;
  private @Getter volatile State state = State.NEW;
  private @Getter Throwable failureCause = null;
  private @Getter @Setter boolean shutdownGroup = true;
  private EventLoopGroup group = null;
  private Channel serverChannel;
  private Channel clientChannel;

  private final CountDownLatch latch = new CountDownLatch(1);

  public LocalClientCouple(ChannelInitializer<LocalChannel> serverInit, ChannelInitializer<LocalChannel> clientInit, EventLoopGroup group) {
    this(serverInit, clientInit);
    Preconditions.checkArgument(!group.isShutdown());
    this.group = group;
    this.shutdownGroup = false;
  }

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
            } else {
              fail(cf.cause());
            }
            latch.countDown();
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
    latch.await();
  }

  public void awaitRunning(long timeoutMillis) throws InterruptedException {
    latch.await(timeoutMillis, TimeUnit.MILLISECONDS);
  }

  private void fail(Throwable cause) {
    this.state = State.FAILED;
    this.failureCause = cause;
    _close();
  }

  @Override
  public void close() {
    if (state == State.CLOSED || state == State.FAILED) {
      return;
    }
    state = State.CLOSED;
    _close();
  }

  private void _close() {
    if (serverChannel != null) {
      serverChannel.close().awaitUninterruptibly();
    }
    if (clientChannel != null) {
      clientChannel.close();
    }
    if (shutdownGroup) {
      group.shutdownGracefully();
    }
    latch.countDown();
  }


}
