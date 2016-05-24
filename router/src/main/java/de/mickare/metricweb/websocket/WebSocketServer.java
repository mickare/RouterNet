package de.mickare.metricweb.websocket;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.mickare.metricweb.PushService;
import de.mickare.metricweb.event.ClosedWebConnectionEvent;
import de.mickare.metricweb.event.OpenedWebConnectionEvent;
import de.mickare.metricweb.protocol.WebProtocol;
import de.rennschnitzel.net.netty.PipelineUtils;
import de.rennschnitzel.net.router.Router;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public @RequiredArgsConstructor class WebSocketServer extends AbstractIdleService {

  private @NonNull @Getter final Logger logger;

  private @Getter final int port;
  private @Getter final boolean SSL;

  private EventLoopGroup group;
  private Channel serverChannel;

  private final Set<WebConnection> connections = Sets.newConcurrentHashSet();
  private final Map<String, PushService> streamServices = Maps.newConcurrentMap();

  private @NonNull @Getter final WebProtocol protocol;

  WebConnection newConnection(final Channel channel) {
    final WebConnection con = new WebConnection(this, channel);
    channel.closeFuture().addListener(f -> {
      connections.remove(con);
      Router.getInstance().getEventBus().post(new ClosedWebConnectionEvent(con));
    });
    this.connections.add(con);
    Router.getInstance().getEventBus().post(new OpenedWebConnectionEvent(con));
    return con;
  }

  @Override
  protected void startUp() throws Exception {

    // Configure SSL.
    final SslContext sslCtx;
    if (SSL) {
      SelfSignedCertificate ssc = new SelfSignedCertificate();
      sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
    } else {
      sslCtx = null;
    }

    group = PipelineUtils.newEventLoopGroup(1,
        new ThreadFactoryBuilder().setNameFormat("MetricWeb WebSocket IO Thread #%1$d").build());

    try {
      ServerBootstrap b = new ServerBootstrap();
      b.group(group);
      b.channel(PipelineUtils.getServerChannelClass());
      b.handler(new LoggingHandler(LogLevel.INFO));

      b.childHandler(new ChannelInitializer<SocketChannel>() {
        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
          final ChannelPipeline pipeline = ch.pipeline();
          if (sslCtx != null) {
            pipeline.addLast(sslCtx.newHandler(ch.alloc()));
          }
          pipeline.addLast(new HttpServerCodec());
          pipeline.addLast(new HttpObjectAggregator(65536));
          pipeline.addLast(new WebSocketServerCompressionHandler());
          pipeline.addLast(new WebSocketServerProtocolHandler("/ws", null, true));
          pipeline.addLast(new WebProtocolDecoder(protocol));
          pipeline.addLast(new WebProtocolEncoder(protocol));
          pipeline.addLast(new WebSocketConnectionHandler(WebSocketServer.this));
        }
      });
      this.serverChannel = b.bind(port).sync().channel();

      this.getLogger().info("Listening on " + (SSL ? "https" : "http") + " port: " + port);

    } catch (Exception e) {
      group.shutdownGracefully();
      throw e;
    }
  }

  @Override
  protected void shutDown() throws Exception {
    if (this.serverChannel != null) {
      serverChannel.closeFuture().sync();
    }

    group.shutdownGracefully().awaitUninterruptibly();
  }

  public Set<WebConnection> getConnections() {
    return Collections.unmodifiableSet(this.connections);
  }

  public void register(PushService service) {
    if (this.streamServices.putIfAbsent(service.getName(), service) != null) {
      throw new IllegalStateException(service.getName() + " already registered");
    }
  }

}
