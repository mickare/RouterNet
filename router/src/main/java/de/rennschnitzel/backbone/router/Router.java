package de.rennschnitzel.backbone.router;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.rennschnitzel.backbone.Owner;
import de.rennschnitzel.backbone.netty.BaseChannelInitializer;
import de.rennschnitzel.backbone.router.api.JavaPlugin;
import de.rennschnitzel.backbone.router.command.CommandManager;
import de.rennschnitzel.backbone.router.config.ConfigFile;
import de.rennschnitzel.backbone.router.config.Settings;
import de.rennschnitzel.backbone.router.netty.PipelineUtils;
import io.netty.bootstrap.ServerBootstrap;
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
  private ExecutorService scheduler = null;
  private final Map<String, JavaPlugin> plugins = Maps.newHashMap();
  @Getter
  private final EventBus eventBus = new EventBus();

  @Getter
  private HostAndPort hostAndPort;

  private EventLoopGroup eventLoops;

  @Getter
  private final RouterNetwork network;

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
  }

  @Override
  protected Executor executor() {
    return MoreExecutors.directExecutor();
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

    // Start Scheduler
    this.scheduler = Executors.newCachedThreadPool(
        new ThreadFactoryBuilder().setNameFormat("Niflhel Pool Thread #%1$d").build());

    // Start Socket Server
    hostAndPort =
        HostAndPort.fromString(this.configFile.getConfig().getRouterSettings().getHostAndPort())
            .withDefaultPort(791);

    // Start Plugins
    List<JavaPlugin> plugs = Lists.newArrayList();

    for (JavaPlugin p : plugs) {
      p.startAsync();
      p.awaitRunning(10, TimeUnit.SECONDS);
      if (p.state() == Service.State.FAILED) {
        getLogger().log(Level.SEVERE, "Plugin failed", p.failureCause());
        continue;
      }
      this.plugins.put(p.getName(), p);
    }


    eventLoops = PipelineUtils.newEventLoopGroup(0,
        new ThreadFactoryBuilder().setNameFormat("Netty IO Thread #%1$d").build());



    logger.info("Server started");
  }

  private void startNetty() {
    ServerBootstrap b = new ServerBootstrap();
    b.group(eventLoops);
    b.option(ChannelOption.SO_REUSEADDR, true);
    b.childHandler(new BaseChannelInitializer(getLogger(), new RouterHandshakeHandler(this)));
    b.localAddress(
        new InetSocketAddress(this.hostAndPort.getHostText(), this.hostAndPort.getPort()));
    b.bind();
  }

  @Override
  protected void shutDown() throws Exception {
    logger.info("Stopping...");

    // Stop Plugins
    for (JavaPlugin p : this.plugins.values()) {
      p.stopAsync();
      p.awaitTerminated(5, TimeUnit.SECONDS);
      if (p.state() == Service.State.FAILED) {
        getLogger().log(Level.SEVERE, "Plugin failed", p.failureCause());
      }
    }

    // Stop Scheduler
    if (this.scheduler != null) {
      this.scheduler.shutdown();
      if (!this.scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        this.scheduler.shutdownNow();
      }
    }

    StringBuilder sb = new StringBuilder();
    sb.append("\n****************************************************");
    sb.append("\nStop");
    sb.append("\n****************************************************");
    logger.info(sb.toString());
  }

  public JavaPlugin getPlugin(String name) {
    return this.plugins.get(name);
  }

}
