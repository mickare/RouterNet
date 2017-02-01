package de.mickare.routernet.router;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.mickare.routernet.Owner;
import de.mickare.routernet.RouterNetwork;
import de.mickare.routernet.config.ConfigFile;
import de.mickare.routernet.core.Node.HomeNode;
import de.mickare.routernet.core.login.AuthenticationFactory;
import de.mickare.routernet.core.login.RouterAuthentication;
import de.mickare.routernet.core.login.RouterLoginEngine;
import de.mickare.routernet.metric.Metric;
import de.mickare.routernet.netty.ConnectionHandler;
import de.mickare.routernet.netty.LoginHandler;
import de.mickare.routernet.netty.PipelineUtils;
import de.mickare.routernet.protocol.NetworkProtocol.NodeMessage;
import de.mickare.routernet.router.command.CommandManager;
import de.mickare.routernet.router.config.Settings;
import de.mickare.routernet.router.packet.RouterPacketHandler;
import de.mickare.routernet.router.plugin.Plugin;
import de.mickare.routernet.router.plugin.PluginManager;
import de.mickare.routernet.util.BuildInfo;
import de.mickare.routernet.util.FutureUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import jline.console.ConsoleReader;
import lombok.Getter;

public class Router extends AbstractIdleService implements Owner {

  private static @Getter Router instance = null;

  private final @Getter Logger logger;
  private final @Getter BuildInfo buildInfo;

  private final @Getter ConsoleReader console;
  private final @Getter CommandManager commandManager;
  private final @Getter ConfigFile<Settings> configFile;
  // private final @Getter ScheduledExecutorService scheduler;
  private final @Getter PluginManager pluginManager;
  private @Getter HostAndPort address;
  private final @Getter RouterNetwork network;

  private @Getter EventLoopGroup eventLoop = null;
  private Channel listener = null;
  private @Getter RouterAuthentication authentication = null;
  private @Getter Metric metric;

  private @Getter final File pluginsFolder;

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
    String pluginsFolderPath = this.configFile.getConfig().getPluginFolder();
    Preconditions.checkNotNull(pluginsFolderPath);
    this.pluginsFolder = new File(pluginsFolderPath).getAbsoluteFile();

    this.commandManager = new CommandManager(this);

    this.buildInfo = new BuildInfo(Router.class.getClassLoader(), "build.properties");
    Preconditions.checkNotNull(buildInfo.getName(),
        "build properties does not contain name of build");

    this.pluginManager = new PluginManager(this);

    /*
     * this.scheduler = MoreExecutors.getExitingScheduledExecutorService( new
     * ScheduledThreadPoolExecutor(0, new ThreadFactoryBuilder().setNameFormat(
     * "Router Pool Thread #%1$d").build()), 10, TimeUnit.SECONDS);
     */

    // Init EventLoop
    this.eventLoop = PipelineUtils.newEventLoopGroup(0,
        new ThreadFactoryBuilder().setNameFormat("Netty IO Thread #%1$d").build());

    HomeNode home = new HomeNode(this.configFile.getConfig().getRouterSettings().getHome().getId(),
        this.getConfig().getRouterSettings().getHome().getNamespaces());
    home.setType(NodeMessage.Type.ROUTER);
    this.network = new RouterNetwork(this, this.eventLoop, home);
    home.setName(this.getConfig().getRouterSettings().getHome().getName());
  }

  public String getName() {
    return this.buildInfo.getName();
  }

  public String getVersion() {
    return this.buildInfo.getBuildVersion();
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
    sb.append("\n").append(this.getName()).append(" ").append(this.getVersion());
    sb.append("\nBuild: ").append(this.buildInfo.getBuildDate())//
        .append(" ").append(this.buildInfo.getJDK());
    sb.append("\n       ").append(this.buildInfo.getBuildOs()).append(" by ")//
        .append(this.buildInfo.getBuildUser());
    sb.append("\n****************************************************");
    logger.info(sb.toString());

    // Load plugins
    File updateFolder = new File(this.pluginsFolder, "update");
    if(updateFolder.isDirectory()) {
    	this.pluginManager.updatePlugins(this.pluginsFolder, updateFolder);
    } else {
    	updateFolder.mkdirs(); // makes plugin folder
    }
    this.pluginManager.detectPlugins(this.pluginsFolder);
    this.pluginManager.loadPlugins();

    // Start Socket Server
    this.address = HostAndPort//
        .fromString(this.configFile.getConfig().getRouterSettings().getAddress())
        .withDefaultPort(791);

    this.authentication = AuthenticationFactory
        .newPasswordForRouter(this.getConfigFile().getConfig().getRouterSettings().getPassword());

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
    b.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
        new WriteBufferWaterMark(16 * 1024, 32 * 1024));

    b.childHandler(PipelineUtils.baseInitAnd(ch -> {
      final ChannelPipeline p = ch.pipeline();
      p.addFirst("trafficMetric", this.metric.getChannelTrafficHandler());
      p.addLast("packetMetric", this.metric.getPacketTrafficHandler());
      p.addLast(
          new LoginHandler(new RouterLoginEngine(Router.this.getNetwork(), this.authentication),
              FutureUtils.newPromise()));
      p.addLast(new ConnectionHandler(Router.this.getNetwork(), new RouterPacketHandler()));
    }));

    //b.localAddress(new InetSocketAddress(this.address.getHostText(), this.address.getPort()));
    b.bind(this.address.getPort()).addListener(new ChannelFutureListener() {
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
    for (Plugin plugin : this.pluginManager.getPlugins()) {
      try {
        plugin.disable();
      } catch (Throwable t) {
        getLogger().log(Level.SEVERE,
            "Exception disabling plugin " + plugin.getDescription().getName(), t);
      }
      plugin.getExecutorService().shutdown();
    }

    // Close EventLoopGroup
    getLogger().info("Closing EventLoop threads");
    eventLoop.shutdownGracefully();
    try {
      eventLoop.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    } catch (InterruptedException ex) {
    }

    StringBuilder sb = new StringBuilder();
    sb.append("\n****************************************************");
    sb.append("\nStop");
    sb.append("\n****************************************************");
    logger.info(sb.toString());
  }

}
