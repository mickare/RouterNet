package de.rennschnitzel.net;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.client.ClientSettings;
import de.rennschnitzel.net.client.ConfigFile;
import de.rennschnitzel.net.client.connection.ConnectionService;
import de.rennschnitzel.net.client.testing.TestingConnectionService;
import de.rennschnitzel.net.client.testing.TestingFramework;
import de.rennschnitzel.net.core.Node.HomeNode;
import lombok.Getter;

@Getter
public class NetClient {

  private Logger logger = Logger.getGlobal();
  private File directory;
  private ConfigFile<ClientSettings> configFile;

  private HomeNode home;
  private Network network;
  private ConnectionService connectionService;
  private ScheduledExecutorService executor;

  private TestingFramework test = null;

  public NetClient() {}

  public void init(Logger logger, File directory, ScheduledExecutorService executor) {
    Preconditions.checkNotNull(logger);
    Preconditions.checkArgument(directory.isDirectory());
    Preconditions.checkNotNull(executor);
    this.logger = logger;
    this.directory = directory;
    this.executor = executor;
    this.configFile = ConfigFile.create(new File(directory, "settings.json"), ClientSettings.class);
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

  public void enable() throws Exception {
    this.configFile.reload();

    this.home = new HomeNode(getConfig().getNode().getId());
    network = new Network(this);
    Net.setNetwork(network);

    TestingFramework.Mode testMode = this.getConfig().getConnection().getTesting();
    if (testMode != null && testMode != TestingFramework.Mode.NONE) {
      this.test = new TestingFramework(this, testMode);
      this.connectionService = new TestingConnectionService(this.test);
      this.connectionService.startAsync();
    }

  }

  public void disable() throws Exception {
    this.connectionService.stopAsync();
  }

}
