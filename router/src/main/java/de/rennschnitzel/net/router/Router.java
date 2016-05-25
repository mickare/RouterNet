package de.rennschnitzel.net.router;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.rennschnitzel.net.Owner;
import de.rennschnitzel.net.RouterNetwork;
import de.rennschnitzel.net.core.Node.HomeNode;
import de.rennschnitzel.net.core.login.AuthenticationFactory;
import de.rennschnitzel.net.core.login.RouterAuthentication;
import de.rennschnitzel.net.core.login.RouterLoginEngine;
import de.rennschnitzel.net.netty.ConnectionHandler;
import de.rennschnitzel.net.netty.LoginHandler;
import de.rennschnitzel.net.netty.PipelineUtils;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeMessage;
import de.rennschnitzel.net.router.command.CommandManager;
import de.rennschnitzel.net.router.config.ConfigFile;
import de.rennschnitzel.net.router.config.Settings;
import de.rennschnitzel.net.router.metric.Metric;
import de.rennschnitzel.net.router.packet.RouterPacketHandler;
import de.rennschnitzel.net.router.plugin.PluginManager;
import de.rennschnitzel.net.util.FutureUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import jline.console.ConsoleReader;
import lombok.Getter;

public class Router extends AbstractIdleService implements Owner {

  private static @Getter Router instance = null;

  private final @Getter Logger logger;
  private final @Getter Properties properties;
  private final @Getter String name, version, builddate;
  private final @Getter ConsoleReader console;
  private final @Getter CommandManager commandManager;
  private final @Getter ConfigFile<Settings> configFile;
  private final @Getter ScheduledExecutorService scheduler;
  private final @Getter PluginManager pluginManager = new PluginManager(this);
  private @Getter HostAndPort address;
  private final @Getter RouterNetwork network;

  private @Getter EventLoopGroup eventLoop = null;
  private Channel listener = null;
  private @Getter RouterAuthentication authentication = null;
  private @Getter Metric metric;

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

    HomeNode home = new HomeNode(this.configFile.getConfig().getRouterSettings().getHome().getId(),
        this.getConfig().getRouterSettings().getHome().getNamespaces());
    home.setType(NodeMessage.Type.ROUTER);
    this.network = new RouterNetwork(this, home);
    home.setName(this.getConfig().getRouterSettings().getHome().getName());
  }

  public Settings getConfig() {
    return this.getConfigFile().getConfig();
  }

  public EventBus getEventBus() {
    return this.network.getEventBus();
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
    this.address = HostAndPort//
        .fromString(this.configFile.getConfig().getRouterSettings().getAddress())
        .withDefaultPort(791);

    this.authentication = AuthenticationFactory
        .newPasswordForRouter(this.getConfigFile().getConfig().getRouterSettings().getPassword());

    // Init EventLoop
    this.eventLoop = PipelineUtils.newEventLoopGroup(0,
        new ThreadFactoryBuilder().setNameFormat("Netty IO Thread #%1$d").build());
    this.metric = new Metric(eventLoop);

    // Enable plugins
    this.pluginManager.enablePlugins();

    startNetty();

    logger.info("Router started.");
  }

  private void startNetty() {

    ServerBootstrap b = new ServerBootstrap();
    b.group(eventLoop);
    b.channel(PipelineUtils.getServerChannelClass());
    b.option(ChannelOption.SO_REUSEADDR, true);

    b.childHandler(PipelineUtils.baseInitAnd(ch -> {
      final ChannelPipeline p = ch.pipeline();
      p.addFirst("trafficMetric", this.metric.getChannelTrafficHandler());
      p.addLast("packetMetric", this.metric.getPacketTrafficHandler());
      p.addLast(
          new LoginHandler(new RouterLoginEngine(Router.this.getNetwork(), this.authentication),
              FutureUtils.newPromise()));
      p.addLast(new ConnectionHandler(Router.this.getNetwork(), new RouterPacketHandler()));
    }));

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
  }

  
  @Override
  protected void shutDown() throws Exception {
    logger.info("Stopping...");
    
    // Close main server socket
    getLogger().log(Level.INFO, "Closing listener {0}", listener);
    try {
      listener.close().syncUninterruptibly();
    } catch (ChannelException ex) {
      getLogger().severe("Could not close listener thread");
    }

    // Disable plugins
    getLogger().info("Disabling plugins");
    this.pluginManager.disablePlugins();
    
    // Close EventLoopGroup
    getLogger().info("Closing IO threads");
    eventLoop.shutdownGracefully();
    try {
      eventLoop.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    } catch (InterruptedException ex) {
    }
        
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

}
