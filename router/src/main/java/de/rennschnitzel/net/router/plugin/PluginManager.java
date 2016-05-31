package de.rennschnitzel.net.router.plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.rennschnitzel.net.router.Router;
import lombok.Getter;

public class PluginManager {

  private static Gson GSON = new GsonBuilder().create();

  @Getter
  private final Router router;

  private final Map<String, Plugin> plugins = new LinkedHashMap<>();
  private Map<String, PluginDescription> toLoad = new HashMap<>();

  public PluginManager(Router router) {
    Preconditions.checkNotNull(router);
    this.router = router;
  }

  public Collection<Plugin> getPlugins() {
    return Collections.unmodifiableCollection(this.plugins.values());
  }


  public void loadPlugins() {
    Map<PluginDescription, Boolean> pluginStatuses = new HashMap<>();
    for (Map.Entry<String, PluginDescription> entry : toLoad.entrySet()) {
      PluginDescription plugin = entry.getValue();
      if (!enablePlugin(pluginStatuses, new Stack<PluginDescription>(), plugin)) {
        router.getLogger().log(Level.WARNING, "Failed to enable {0}", entry.getKey());
      }
    }
    toLoad.clear();
    toLoad = null;
  }

  public void enablePlugins() {
    for (Plugin plugin : plugins.values()) {
      try {
        plugin.onEnable();
        router.getLogger().log(Level.INFO, "Enabled plugin {0} version {1} by {2}",
            new Object[] {plugin.getDescription().getName(), plugin.getDescription().getVersion(),
                plugin.getDescription().getAuthor()});
      } catch (Throwable t) {
        router.getLogger().log(Level.WARNING,
            "Exception encountered when loading plugin: " + plugin.getDescription().getName(), t);
      }
    }
  }

  private boolean enablePlugin(Map<PluginDescription, Boolean> pluginStatuses,
      Stack<PluginDescription> dependStack, PluginDescription plugin) {
    if (pluginStatuses.containsKey(plugin)) {
      return pluginStatuses.get(plugin);
    }

    // combine all dependencies for 'for loop'
    Set<String> dependencies = new HashSet<>();
    dependencies.addAll(plugin.getDepends());
    dependencies.addAll(plugin.getSoftDepends());

    // success status
    boolean status = true;

    // try to load dependencies first
    for (String dependName : dependencies) {
      PluginDescription depend = toLoad.get(dependName);
      Boolean dependStatus = (depend != null) ? pluginStatuses.get(depend) : Boolean.FALSE;

      if (dependStatus == null) {
        if (dependStack.contains(depend)) {
          StringBuilder dependencyGraph = new StringBuilder();
          for (PluginDescription element : dependStack) {
            dependencyGraph.append(element.getName()).append(" -> ");
          }
          dependencyGraph.append(plugin.getName()).append(" -> ").append(dependName);
          router.getLogger().log(Level.WARNING, "Circular dependency detected: {0}",
              dependencyGraph);
          status = false;
        } else {
          dependStack.push(plugin);
          dependStatus = this.enablePlugin(pluginStatuses, dependStack, depend);
          dependStack.pop();
        }
      }

      // only fail if this wasn't a soft dependency
      if (dependStatus == Boolean.FALSE && plugin.getDepends().contains(dependName)) {
        router.getLogger().log(Level.WARNING, "{0} (required by {1}) is unavailable",
            new Object[] {String.valueOf(dependName), plugin.getName()});
        status = false;
      }

      if (!status) {
        break;
      }
    }

    // do actual loading
    if (status) {
      try {
        URLClassLoader loader = new PluginClassloader(new URL[] {plugin.getFile().toURI().toURL()});
        Class<?> main = loader.loadClass(plugin.getMain());
        Plugin clazz = (Plugin) main.getDeclaredConstructor().newInstance();

        clazz.init(router, plugin);
        plugins.put(plugin.getName(), clazz);
        clazz.onLoad();
        router.getLogger().log(Level.INFO, "Loaded plugin {0} version {1} by {2}",
            new Object[] {plugin.getName(), plugin.getVersion(), plugin.getAuthor()});
      } catch (Throwable t) {
        router.getLogger().log(Level.WARNING, "Error enabling plugin " + plugin.getName(), t);
      }
    }

    pluginStatuses.put(plugin, status);
    return status;
  }

  /**
   * Load all plugins from the specified folder.
   *
   * @param folder the folder to search for plugins in
   */
  public void detectPlugins(File folder) {
    Preconditions.checkNotNull(folder, "folder");
    Preconditions.checkArgument(folder.isDirectory(), "Must load from a directory");

    for (File file : folder.listFiles()) {
      if (file.isFile() && file.getName().endsWith(".jar")) {
        try (JarFile jar = new JarFile(file)) {
          JarEntry pdf = jar.getJarEntry("plugin.json");

          Preconditions.checkNotNull(pdf, "Plugin must have a plugin.json");

          try (InputStream in = jar.getInputStream(pdf);
              Reader reader = new InputStreamReader(in)) {
            PluginDescription desc = GSON.fromJson(reader, PluginDescription.class);
            Preconditions.checkNotNull(desc.getName(), "Plugin from %s has no name", file);
            Preconditions.checkNotNull(desc.getMain(), "Plugin from %s has no main", file);

            desc.setFile(file);
            toLoad.put(desc.getName(), desc);
          }
        } catch (Exception ex) {
          router.getLogger().log(Level.WARNING, "Could not load plugin from file " + file, ex);
        }
      }
    }
  }


}
