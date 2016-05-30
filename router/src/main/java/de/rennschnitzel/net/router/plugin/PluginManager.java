package de.rennschnitzel.net.router.plugin;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Level;

import com.google.common.base.Preconditions;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.MutableClassToInstanceMap;
import com.google.common.util.concurrent.Service.State;

import de.rennschnitzel.net.router.Router;
import lombok.Getter;

public class PluginManager {

  @Getter
  private final Router router;
  private final ClassToInstanceMap<Plugin> plugins = MutableClassToInstanceMap.create();

  private boolean loaded = false;

  private @Getter final File pluginDir;

  public PluginManager(Router router) {
    Preconditions.checkNotNull(router);
    this.router = router;

    this.pluginDir = new File(router.getConfig().getPluginFolder());
    if (!pluginDir.isDirectory()) {
      pluginDir.mkdirs();
    }
  }

  private static String getName(Class<? extends Plugin> c) {
    String name = null;
    Plugin.Name a = c.getAnnotation(Plugin.Name.class);
    if (a != null) {
      name = a.value();
    }
    return name != null ? name : c.getName();
  }

  public synchronized void loadPlugins() {
    Preconditions.checkState(this.loaded == false, "already loaded");
    Preconditions.checkState(router.state() == State.STARTING);

    if (this.pluginDir.isDirectory()) {

      try {
        File[] files =
            this.pluginDir.listFiles(file -> file.getPath().toLowerCase().endsWith(".jar"));
        URL[] urls = new URL[files.length];
        for (int i = 0; i < files.length; ++i) {
          urls[i] = files[i].toURI().toURL();
        }
        URLClassLoader ucl = new URLClassLoader(urls);


        ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class, ucl);
        loader.forEach(this::loadPlugin);

        Iterator<Plugin> it = loader.iterator();
        while (it.hasNext()) {

          try {
            loadPlugin(it.next());
          } catch (ServiceConfigurationError sce) {
            router.getLogger().log(Level.SEVERE, "Failed to load plugin: ", sce);
          }

        }

      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }

    }

  }

  private void loadPlugin(final Plugin plugin) {
    final Class<? extends Plugin> pluginClass = plugin.getClass();
    try {
      if (this.plugins.containsKey(plugin.getClass())) {
        router.getLogger().warning("Plugin \"" + getName(pluginClass) + "\" already loaded!");
        return;
      }
      plugin.init(router);
      plugin.onLoad();
      this.plugins.put(pluginClass, plugin);
    } catch (Exception e) {
      router.getLogger().log(Level.SEVERE,
          "Failed to load plugin: \"" + getName(pluginClass) + "\"", e);
    }
  }



  public synchronized void enablePlugins() {
    Preconditions.checkState(router.state() == State.STARTING || router.state() == State.RUNNING);
    for (Plugin plugin : plugins.values()) {
      plugin.enable();
    }
  }

  public synchronized void disablePlugins() {
    Preconditions.checkState(router.state() == State.STOPPING);
    for (Plugin plugin : Lists.reverse(Lists.newArrayList(plugins.values()))) {
      plugin.disable();
    }
  }

  public synchronized Plugin getPlugin(String name) {
    for (Plugin plugin : plugins.values()) {
      if (plugin.getName().equalsIgnoreCase(name)) {
        return plugin;
      }
    }
    return null;
  }

  public <P extends Plugin> P getPlugin(Class<P> pluginClass) {
    return this.plugins.getInstance(pluginClass);
  }
  
  public Set<Plugin> getPlugins() {
    return ImmutableSet.copyOf(this.plugins.values());
  }

}
