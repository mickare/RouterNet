package de.rennschnitzel.net.router.plugin;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Service.State;

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

  public synchronized void loadPlugins() {
    Preconditions.checkState(this.loaded == false, "already loaded");
    Preconditions.checkState(router.state() == State.STARTING);

    // Define all plugins here in correct order...
    // add(...);


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
