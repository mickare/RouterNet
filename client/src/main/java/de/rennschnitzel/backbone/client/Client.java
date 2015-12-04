package de.rennschnitzel.backbone.client;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractScheduledService;

import de.rennschnitzel.backbone.api.network.Connection;
import de.rennschnitzel.backbone.api.network.ConnectionFuture;
import de.rennschnitzel.backbone.api.network.RouterInfo;
import de.rennschnitzel.backbone.client.util.UUIDContainer;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.AuthSuccess;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.Server;
import de.rennschnitzel.backbone.netty.BackboneChannelInitializer;
import de.rennschnitzel.backbone.util.FutureUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;

public class Client extends AbstractScheduledService {

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
  private final Logger logger;
  @Getter
  private final RouterInfo router;
  private final Set<String> namespaces = Sets.newConcurrentHashSet();
  private final Set<String> namespacesView = Collections.unmodifiableSet(namespaces);
  @Getter
  private final Server.Type type;

  @Getter
  private final UUIDContainer uuid = new UUIDContainer();

  private EventLoopGroup group = null;

  private ConnectionFuture connection = null;

  public Client(Context context) {
    Preconditions.checkNotNull(context);
    this.logger = context.logger;
    this.router = context.router;
    this.namespaces.addAll(context.namespaces);
    this.type = context.type;
  }

  public Set<String> getNamespaces() {
    return namespacesView;
  }

  public boolean isConnected() {
    return this.connection != null && this.connection.isSuccess();
  }

  private synchronized ConnectionFuture tryConnect() {
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
    connection = doConnect(router);
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
    b.handler(new BackboneChannelInitializer(logger, handshake.getHandler()));
    b.connect(router.getHost(), router.getPort()).addListener(handshake);


    FutureUtils.onSuccess(handshake, (con) -> getLogger().info("Connected!"));
    
    return handshake;
  }

  @Override
  protected void startUp() throws Exception {
    group = new NioEventLoopGroup();
  }
  
  protected String serviceName() {
    return "Client";
  }

  @Override
  protected void shutDown() throws Exception {
    if (connection != null) {
      connection.cancelOrClose();
    }
    if (group != null) {
      group.shutdownGracefully();
    }
  }

  private int failedConnections = 0;

  @Override
  protected void runOneIteration() throws Exception {
    ConnectionFuture f = tryConnect();
    try {
      Connection con = f.get(3, TimeUnit.SECONDS);
    } catch (Exception e) {
      f.cancelOrClose();
      if (failedConnections++ % 60 == 0) {
        getLogger().info("Failed to connect to router!");
      }
    }
  }

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedDelaySchedule(1, 2, TimeUnit.SECONDS);
  }
}
