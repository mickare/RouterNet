package de.mickare.routernet.router.plugin;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.mickare.routernet.router.Router;
import de.mickare.routernet.util.LazyCache;
import lombok.Getter;

public abstract class Plugin {

  private @Getter PluginDescription description;
  private @Getter Router router;
  private @Getter File file;
  private @Getter Logger logger;
  private final LazyCache<ScheduledExecutorService> service =
      LazyCache.of(this::createExecutorService);

  private @Getter boolean enabled = false;

  final void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  protected abstract void onLoad() throws Exception;

  public final void enable() throws Exception {
    try {
      this.enabled = true;
      this.onEnable();
    } catch (Exception e) {
      this.enabled = false;
      throw e;
    }
  }

  public final void disable() throws Exception {
    try {
      this.enabled = false;
      this.onDisable();
      for (java.util.logging.Handler handler : getLogger().getHandlers()) {
        handler.close();
      }
    } catch (Exception e) {
      throw e;
    }
  }

  protected abstract void onEnable() throws Exception;

  protected abstract void onDisable() throws Exception;

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

  private ScheduledExecutorService createExecutorService() {
    String name = (getDescription() == null) ? "unknown" : getDescription().getName();

    // return new ScheduledThreadPoolExecutor(1,
    // new ThreadFactoryBuilder().setNameFormat(name + " Pool Thread #%1$d").build());
    return Executors.newScheduledThreadPool(1,
        new ThreadFactoryBuilder().setNameFormat(name + " Pool Thread #%1$d").build());
  }

  public ScheduledExecutorService getExecutorService() {
    return service.getUnchecked();
  }

}
