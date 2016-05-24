package de.rennschnitzel.net.router.plugin;

import java.util.logging.Level;
import java.util.logging.Logger;

import de.rennschnitzel.net.router.Router;
import lombok.Getter;

public abstract class JavaPlugin {

  @Getter
  private Router router;

  @Getter
  private String name, version, author;

  @Getter
  private Logger logger;

  @Getter
  private boolean enabled = false;

  public JavaPlugin() {
    this.logger = new PluginLogger(this);
  }

  final void init(Router router) {
    Plugin plugin = this.getClass().getDeclaredAnnotation(Plugin.class);
    if (plugin != null) {
      this.name = plugin.name();
      this.version = plugin.version();
      this.author = plugin.author();
    }
    if (this.name == null) {
      this.name = this.getClass().getName();
    }
    this.router = router;
  }

  protected synchronized final void enable() {
    if (this.enabled) {
      return;
    }
    try {
      router.getLogger().info("Enabling \"" + getName() + "\"...");
      this.onEnable();
      this.enabled = true;
      router.getLogger().info("Plugin \"" + getName() + "\" enabled.");
    } catch (Exception e) {
      router.getLogger().log(Level.SEVERE, "Exception while enabling plugin: " + e.getMessage(), e);
    }
  }

  protected synchronized final void disable() {
    if (!this.enabled) {
      return;
    }
    try {
      router.getLogger().info("Disabling \"" + getName() + "\"...");
      this.enabled = false;
      this.onDisable();
      router.getLogger().info("Plugin \"" + getName() + "\" disabled.");
    } catch (Exception e) {
      router.getLogger().log(Level.SEVERE, "Exception while disabling plugin: " + e.getMessage(),
          e);
    }
  }

  protected abstract void onEnable() throws Exception;

  protected abstract void onDisable() throws Exception;

  protected abstract void onLoad() throws Exception;

}
