package de.rennschnitzel.net.router.plugin;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.rennschnitzel.net.router.Router;
import de.rennschnitzel.net.util.LazyCache;
import lombok.Getter;

public abstract class Plugin {

  private @Getter PluginDescription description;
  private @Getter Router router;
  private @Getter File file;
  private @Getter Logger logger;
  private final LazyCache<ListeningScheduledExecutorService> service =
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

  private ListeningScheduledExecutorService createExecutorService() {
    String name = (getDescription() == null) ? "unknown" : getDescription().getName();
    return MoreExecutors.listeningDecorator(new ScheduledThreadPoolExecutor(0,
        new ThreadFactoryBuilder().setNameFormat(name + " Pool Thread #%1$d").build()));
  }

  public ListeningScheduledExecutorService getExecutorService() {
    return service.getUnchecked();
  }

}
