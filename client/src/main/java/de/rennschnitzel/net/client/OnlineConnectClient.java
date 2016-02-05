package de.rennschnitzel.net.client;

import com.google.common.base.Preconditions;
import com.google.common.net.HostAndPort;

import de.rennschnitzel.net.netty.PipelineUtils;
import de.rennschnitzel.net.service.AbstractConnectClient;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalChannel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;


public @RequiredArgsConstructor class OnlineConnectClient extends AbstractConnectClient {

  private final @NonNull HostAndPort addr;
  private final @NonNull ChannelInitializer<Channel> clientInit;
  private @Getter @Setter boolean shutdownGroup = true;
  private EventLoopGroup group = null;
  private Channel clientChannel = null;

  public OnlineConnectClient(HostAndPort addr, ChannelInitializer<Channel> clientInit,
      EventLoopGroup group) {
    this(addr, clientInit);
    Preconditions.checkArgument(!group.isShutdown());
    this.group = group;
    this.shutdownGroup = false;
  }

  @Override
  protected void doConnect() throws Exception {
    if (group == null) {
      group = new DefaultEventLoopGroup();
    }

    Bootstrap cb = new Bootstrap();
    cb.group(group).channel(PipelineUtils.getChannelClass()).handler(clientInit);
    cb.connect(addr.getHostText(), addr.getPort()) //
        .addListener((ChannelFutureListener) f -> {

          if (f.isSuccess()) {
            clientChannel = f.channel();
            notfiyConnected();
          } else {
            notifyFailed(f.cause());
          }

        });

  }

  @Override
  protected void doClose() throws Exception {
    if (clientChannel != null) {
      clientChannel.close();
    }
    if (shutdownGroup && group != null) {
      group.shutdownGracefully();
    }
  }

}
