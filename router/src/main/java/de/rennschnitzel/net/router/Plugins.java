package de.rennschnitzel.net.router;

import java.util.List;

import de.mickare.metricweb.MetricWebPlugin;
import de.rennschnitzel.net.router.plugin.JavaPlugin;

public class Plugins {

  public static void loadPlugins(List<Class<? extends JavaPlugin>> plugins) {

    // Define all plugins here in correct order...

    plugins.add(MetricWebPlugin.class);

  }

}
