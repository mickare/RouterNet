package de.rennschnitzel.net.client;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.Network;
import de.rennschnitzel.net.core.Node.HomeNode;
import lombok.Getter;

public class NetClient {

  @Getter
  private Logger logger = Logger.getGlobal();
  @Getter
  private File directory;
  @Getter
  private ConfigFile<ClientSettings> configFile;

  @Getter
  private HomeNode home;

  @Getter
  private ScheduledExecutorService executor;

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
    Network network = new Network(this);
    Net.
  }

  public void disable() throws Exception {

  }

}
