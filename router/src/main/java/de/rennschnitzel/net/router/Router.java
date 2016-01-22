package de.rennschnitzel.net.router;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.rennschnitzel.net.Owner;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.Node.HomeNode;
import de.rennschnitzel.net.netty.BaseChannelInitializer;
import de.rennschnitzel.net.router.command.CommandManager;
import de.rennschnitzel.net.router.config.ConfigFile;
import de.rennschnitzel.net.router.config.Settings;
import de.rennschnitzel.net.router.netty.NettyConnection;
import de.rennschnitzel.net.router.netty.PipelineUtils;
import de.rennschnitzel.net.router.netty.RouterHandshakeHandler;
import de.rennschnitzel.net.router.plugin.PluginManager;
import de.rennschnitzel.net.util.concurrent.CloseableLock;
import de.rennschnitzel.net.util.concurrent.CloseableReadWriteLock;
import de.rennschnitzel.net.util.concurrent.ReentrantCloseableReadWriteLock;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import jline.console.ConsoleReader;
import lombok.Getter;

public class Router extends AbstractIdleService implements Owner {

  @Getter
  private static Router instance = null;
  @Getter
  private final Logger logger;
  @Getter
  private final Properties properties;
  @Getter
  private final String name, version, builddate;
  @Getter
  private final ConsoleReader console;
  @Getter
  private final CommandManager commandManager;
  @Getter
  private final ConfigFile<Settings> configFile;
  @Getter
  private final ScheduledExecutorService scheduler;
  @Getter
  private final EventBus eventBus = new EventBus();

  @Getter
  private final PluginManager pluginManager = new PluginManager(this);

  @Getter
  private HostAndPort address;


  @Getter
  private final RouterNetwork network;

  private final Map<UUID, NettyConnection> connections = new HashMap<>();
  private final CloseableReadWriteLock connectionLock = new ReentrantCloseableReadWriteLock();

  private EventLoopGroup eventLoops = null;
  private Channel listener = null;

  protected Router(ConsoleReader console, Logger logger) throws IOException {
    Preconditions.checkNotNull(console);
    Preconditions.checkNotNull(logger);

    if (instance != null) {
      throw new IllegalStateException("Already started!");
    }

    instance = this;

    this.console = console;
    this.logger = logger;

    this.configFile = ConfigFile.create(new File("settings.json"), Settings.class);
    this.configFile.saveDefault();

    this.commandManager = new CommandManager(this);

    this.properties = new Properties();
    this.properties.load(Router.class.getClassLoader().getResourceAsStream("project.properties"));

    this.name = this.properties.getProperty("project.name");
    this.version = this.properties.getProperty("project.version");
    this.builddate = this.properties.getProperty("project.builddate");;

    this.scheduler = MoreExecutors.getExitingScheduledExecutorService(
        new ScheduledThreadPoolExecutor(0,
            new ThreadFactoryBuilder().setNameFormat("Router Pool Thread #%1$d").build()),
        10, TimeUnit.SECONDS);

    HomeNode home = new HomeNode(this.configFile.getConfig().getRouterSettings().getUuid());
    this.network = new RouterNetwork(this, home);
  }

  @Override
  protected void startUp() throws Exception {

    StringBuilder sb = new StringBuilder();
    sb.append("Starting...");
    sb.append("\n****************************************************");
    sb.append("\n").append(this.name).append(" ").append(this.version);
    sb.append("\nBuild-Datum ").append(this.builddate);
    sb.append("\n****************************************************");
    logger.info(sb.toString());

    // Load plugins
    this.pluginManager.loadPlugins();

    // Start Socket Server
    address = HostAndPort//
        .fromString(this.configFile.getConfig().getRouterSettings().getAddress())
        .withDefaultPort(791);

    // Enable plugins
    this.pluginManager.enablePlugins();

    eventLoops = PipelineUtils.newEventLoopGroup(0,
        new ThreadFactoryBuilder().setNameFormat("Netty IO Thread #%1$d").build());
    startNetty();

    logger.info("Router started.");
  }

  private void startNetty() {
    ServerBootstrap b = new ServerBootstrap();
    b.group(eventLoops);
    b.option(ChannelOption.SO_REUSEADDR, true);
    b.childHandler(new BaseChannelInitializer(getLogger(), new RouterHandshakeHandler(this)));
    b.localAddress(new InetSocketAddress(this.address.getHostText(), this.address.getPort()));
    b.bind().addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        if (future.isSuccess()) {
          listener = future.channel();
          getLogger().log(Level.INFO, "Listening on {0}", getAddress());
        } else {
          getLogger().log(Level.WARNING, "Could not bind to host " + getAddress(), future.cause());
        }
      }
    });
    logger.info("Server started");
  }

  private void stopNetty() {

    // Close main server socket
    getLogger().log(Level.INFO, "Closing listener {0}", listener);
    try {
      listener.close().syncUninterruptibly();
    } catch (ChannelException ex) {
      getLogger().severe("Could not close listen thread");
    }

    getLogger().info("Closing IO threads");
    eventLoops.shutdownGracefully();
    try {
      eventLoops.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    } catch (InterruptedException ex) {
    }

  }

  @Override
  protected void shutDown() throws Exception {
    logger.info("Stopping...");

    stopNetty();

    // Disable plugins
    getLogger().info("Disabling plugins");
    this.pluginManager.disablePlugins();
    scheduler.shutdown();
    try {
      scheduler.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException ie) {
    }

    StringBuilder sb = new StringBuilder();
    sb.append("\n****************************************************");
    sb.append("\nStop");
    sb.append("\n****************************************************");
    logger.info(sb.toString());
  }

  public Set<Connection> getConnections() {
    try (CloseableLock l = connectionLock.readLock().open()) {
      return ImmutableSet.copyOf(connections.values());
    }
  }

  public Connection getConnection(UUID id) {
    try (CloseableLock l = connectionLock.readLock().open()) {
      return connections.get(id);
    }
  }

  public boolean isConnected(UUID id) {
    Connection con = getConnection(id);
    return con.isValid();
  }

  public void addConnection(NettyConnection connection) {
    try (CloseableLock l = connectionLock.writeLock().open()) {
      connections.put(connection.getId(), connection);
    }
  }

  public void removeConnection(NettyConnection connection) {
    try (CloseableLock l = connectionLock.writeLock().open()) {
      connections.remove(connection.getId(), connection);
    }
  }

}
