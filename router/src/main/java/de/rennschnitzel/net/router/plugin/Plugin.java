package de.rennschnitzel.net.router.plugin;

import java.io.File;
import java.io.InputStream;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.router.Router;
import lombok.Getter;

public abstract class Plugin {

  private @Getter PluginDescription description;
  private @Getter Router router;
  private @Getter File file;
  private @Getter Logger logger;

  private @Getter boolean enabled = false;

  public abstract void onLoad() throws Exception;

  public abstract void onEnable() throws Exception;

  public abstract void onDisable() throws Exception;

  final void init(Router router, PluginDescription description) {
    Preconditions.checkNotNull(router);
    Preconditions.checkNotNull(description);
    this.router = router;
    this.description = description;
    this.file = description.getFile();
    this.logger = new PluginLogger(this);
  }

  public final File getDataFolder() {
    return new File(getRouter().getPluginsFolder(), getDescription().getName());
  }

  public final InputStream getResourceAsStream(String name) {
    return getClass().getClassLoader().getResourceAsStream(name);
  }

  public String getName() {
    return this.description.getName();
  }

}
