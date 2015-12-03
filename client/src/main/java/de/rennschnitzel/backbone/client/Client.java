package de.rennschnitzel.backbone.client;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;

import de.rennschnitzel.backbone.api.Connection;
import de.rennschnitzel.backbone.api.ConnectionFuture;
import de.rennschnitzel.backbone.api.RouterInfo;
import de.rennschnitzel.backbone.client.util.UUIDContainer;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.AuthSuccess;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.Server;
import de.rennschnitzel.backbone.netty.BackboneChannelInitializer;
import de.rennschnitzel.backbone.service.AbstractDirectService;
import de.rennschnitzel.backbone.util.FutureUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.FutureListener;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;

public class Client extends AbstractDirectService {

  @Data
  public static final class Context {
    @NonNull
    private final Logger logger;
    @NonNull
    private final RouterInfo router;
    @NonNull
    private final Set<String> namespaces;
    @NonNull
    private final Server.Type type;
  }

  @Getter
  private final Context context;

  @Getter
  private final UUIDContainer uuid = new UUIDContainer();

  private EventLoopGroup group = null;

  private ConnectionFuture connection = null;

  public Client(Context context) {
    Preconditions.checkNotNull(context);
    this.context = context;
  }

  private ConnectionFuture connect() {
    if (connection != null) {
      try {
        if (connection.isSuccess() && connection.get().isActive()) {
          return connection;
        } else if (!connection.cancel(true)) {
          connection.get(3, TimeUnit.SECONDS).close();
        }
      } catch (Exception e) {
      }
    }
    connection = doConnect(context.router);
    return connection;
  }

  private ConnectionFuture doConnect(RouterInfo router) {
    ClientHandshake handshake = new ClientHandshake(this, router) {
      @Override
      protected RouterConnection upgrade(Channel channel, AuthSuccess authSuccess)
          throws Exception {
        RouterConnection connection = new RouterConnection(authSuccess, Client.this, router);
        channel.pipeline().replace(this.getHandler(), "main", connection);
        return connection;
      }
    };

    Bootstrap b = new Bootstrap();
    b.group(group);
    b.channel(NioSocketChannel.class);
    b.handler(new BackboneChannelInitializer(context.logger, handshake.getHandler()));
    b.connect(router.getHost(), router.getPort()).addListener(handshake);

    return handshake;
  }

  @Override
  protected void onStart() throws Exception {
    group = new NioEventLoopGroup();
    ConnectionFuture f = connect();
    FutureUtils.onSuccess(f, (con) -> getLogger().info("Connected!"));
  }

  @Override
  protected void onStop() throws Exception {
    if (connection != null) {
      connection.cancelOrClose();
    }
    if (group != null) {
      group.shutdownGracefully();
    }
  }

  public Logger getLogger() {
    return context.logger;
  }
}
