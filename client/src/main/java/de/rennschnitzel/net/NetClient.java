package de.rennschnitzel.net;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.Service;

import de.rennschnitzel.net.client.ClientSettings;
import de.rennschnitzel.net.client.ConfigFile;
import de.rennschnitzel.net.client.HomeSettings;
import de.rennschnitzel.net.client.TestFramework;
import de.rennschnitzel.net.client.connection.ConnectService;
import de.rennschnitzel.net.client.connection.LocalConnectService;
import de.rennschnitzel.net.client.connection.OnlineConnectService;
import de.rennschnitzel.net.core.Node.HomeNode;
import de.rennschnitzel.net.core.login.AuthenticationFactory;
import de.rennschnitzel.net.core.login.ClientAuthentication;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeMessage;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;


public @Getter @RequiredArgsConstructor class NetClient {

  @NonNull
  private final NodeMessage.Type type;

  private boolean initialized = false;
  private boolean enabled = false;

  private Logger logger = Logger.getGlobal();
  private File directory;
  private ConfigFile<ClientSettings> configFile;

  private HomeNode home;
  private Network network;
  private ConnectService connectService;
  private ScheduledExecutorService executor;

  private TestFramework testFramework = null;

  private ClientAuthentication authentication = null;

  private HostAndPort routerAddress = null;

  private @Setter @NonNull Runnable shutdownFunction = () -> {
  };
  private @Setter @NonNull Runnable restartFunction = () -> {
  };
  private @Setter @NonNull Consumer<Runnable> syncExecutor = (command) -> this.executor.execute(command);

  public synchronized void init(Logger logger, File directory, ScheduledExecutorService executor) {
    Preconditions.checkState(initialized == false, "NetClient already initialized");
    Preconditions.checkNotNull(logger);
    directory.mkdirs();
    Preconditions.checkArgument(directory.isDirectory());
    Preconditions.checkNotNull(executor);
    this.logger = logger;
    this.directory = directory;
    this.executor = executor;
    this.configFile = ConfigFile.create(new File(directory, "settings.json"), ClientSettings.class);
    saveConfigDefault();
    initialized = true;
  }

  public ClientSettings getConfig() {
    return configFile.getConfig();
  }

  public void reloadConfig() {
    try {
      configFile.reload();
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Could not reload config!\n" + e.getMessage(), e);
    }
  }


  public void saveConfig() {
    try {
      configFile.save();
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Could not save config!\n" + e.getMessage(), e);
    }
  }

  public void saveConfigDefault() {
    try {
      configFile.saveDefault();
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Could not save config!\n" + e.getMessage(), e);
    }
  }

  public synchronized void enable() throws Exception {
    Preconditions.checkState(initialized == true, "NetClient is not initialized");
    Preconditions.checkState(enabled == false, "NetClient is already enabled");
    this.configFile.reload();

    this.authentication =
        AuthenticationFactory.newPasswordForClient(this.getConfig().getConnection().getPassword());

    this.routerAddress = HostAndPort.fromString(this.getConfig().getConnection().getAddress())
        .withDefaultPort(NetConstants.DEFAULT_PORT);


    ConfigFile<HomeSettings> homeSettings =
        ConfigFile.create(new File(this.directory, "home.json"), HomeSettings.class);
    homeSettings.saveDefault();
    HomeSettings home = homeSettings.getConfig();

    UUID id = home.getId();
    if (id == null) {
      id = UUID.randomUUID();
      home.setId(id);
      homeSettings.save();
    }

    this.home = new HomeNode(id, home.getNamespaces());
    this.home.setType(this.type);
    this.home.setName(home.getName());

    network = new Network(this);
    Net.setNetwork(network);

    if (this.getConfig().getConnection().isTestingMode()) {
      this.testFramework = new TestFramework(this);
      this.connectService = new LocalConnectService(this, this.testFramework);
    } else {
      this.testFramework = null;
      this.connectService = new OnlineConnectService(this);
    }
    this.connectService.startAsync();
    this.connectService.awaitRunning(1, TimeUnit.SECONDS);

    if (this.connectService.state() != Service.State.RUNNING) {
      if (this.connectService.state() == Service.State.FAILED) {
        throw new IllegalStateException("connectService failed",
            this.connectService.failureCause());
      } else {
        throw new IllegalStateException("connectService is not running!");
      }
    }

    this.network.resetInstance();

    enabled = true;

    getLogger().info("NetClient enabled!");
  }

  public synchronized void disable() throws Exception {
    Preconditions.checkState(enabled == true, "NetClient is not enabled");
    this.connectService.stopAsync();
    this.testFramework = null;
    enabled = false;

    getLogger().info("NetClient disabled!");
  }

  public boolean isTestMode() {
    return this.testFramework != null;
  }

  public void handleFailConnect(ConnectService service) {
    switch (this.getConfig().getConnection().getFailHandler()) {
      case RETRY:
        break;
      case RESTART:
        service.stopAsync();
        restartFunction.run();
        break;
      case SHUTDOWN:
      default:
        service.stopAsync();
        shutdownFunction.run();
    }
  }

  protected void syncExecute(Runnable command) {
    this.syncExecutor.accept(command);
  }

}
