package de.rennschnitzel.backbone.router.plugin;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.router.Router;
import lombok.Getter;

public abstract class Plugin {

  @Getter
  private final Router router;

  @Getter
  private final String name;

  @Getter
  private final Logger logger;

  @Getter
  private boolean enabled = false;

  public Plugin(Router router, String name) {
    Preconditions.checkNotNull(router);
    Preconditions.checkNotNull(name);
    this.router = router;
    this.name = name;
    this.logger = new PluginLogger(this);
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

}
