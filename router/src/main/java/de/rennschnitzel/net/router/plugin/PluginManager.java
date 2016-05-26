package de.rennschnitzel.net.router.plugin;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Service.State;

import de.rennschnitzel.net.router.Plugins;
import de.rennschnitzel.net.router.Router;
import lombok.Getter;

public class PluginManager {

  @Getter
  private final Router router;
  private final List<Plugin> plugins = Lists.newArrayList();

  private boolean loaded = false;

  public PluginManager(Router router) {
    Preconditions.checkNotNull(router);
    this.router = router;
  }

  private static String getName(Class<? extends Plugin> c) {
    String name = null;
    RouterPlugin a = c.getAnnotation(RouterPlugin.class);
    if (a != null) {
      name = a.name();
    }
    return name != null ? name : c.getName();
  }

  public synchronized void loadPlugins() {
    Preconditions.checkState(this.loaded == false, "already loaded");
    Preconditions.checkState(router.state() == State.STARTING);

    this.plugins.clear();

    List<Class<? extends Plugin>> plugins = Lists.newArrayList();
    Plugins.loadPlugins(plugins);

    for (Class<? extends Plugin> c : plugins) {
      try {
        Plugin plugin = c.newInstance();
        plugin.init(router, c);
        plugin.onLoad();
        this.plugins.add(plugin);
      } catch (InstantiationException | IllegalAccessException e) {
        router.getLogger().log(Level.SEVERE,
            "Can not access standard constructor of plugin \"" + getName(c) + "\"", e);
      } catch (Exception e) {
        router.getLogger().log(Level.SEVERE, "Can not load plugin \"" + getName(c) + "\"", e);
      }
    }


  }

  synchronized void add(Plugin plugin) {
    if (this.plugins.contains(plugin)) {
      return;
    }
    Preconditions.checkArgument(getPlugin(plugin.getName()) == null);
    this.plugins.add(plugin);
  }

  public synchronized void enablePlugins() {
    Preconditions.checkState(router.state() == State.STARTING || router.state() == State.RUNNING);
    for (Plugin plugin : plugins) {
      plugin.enable();
    }
  }

  public synchronized void disablePlugins() {
    Preconditions.checkState(router.state() == State.STOPPING);
    for (Plugin plugin : Lists.reverse(Lists.newArrayList(plugins))) {
      plugin.disable();
    }
  }

  public synchronized Plugin getPlugin(String name) {
    for (Plugin plugin : plugins) {
      if (plugin.getName().equalsIgnoreCase(name)) {
        return plugin;
      }
    }
    return null;
  }

  public Set<Plugin> getPlugins() {
    return Collections.unmodifiableSet(Sets.newHashSet(this.plugins));
  }

}
