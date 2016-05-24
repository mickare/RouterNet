package de.mickare.metricweb;

import de.rennschnitzel.net.router.plugin.JavaPlugin;
import de.rennschnitzel.net.router.plugin.Plugin;
import lombok.Getter;

@Plugin(name = "MetricWeb", version = "0.0.1", author = "mickare")
public class MetricWebPlugin extends JavaPlugin {

  private @Getter PushServiceManager pushServiceManager;

  @Override
  protected void onLoad() throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  protected void onEnable() throws Exception {

    pushServiceManager = new PushServiceManager();

    this.getRouter().getMetric().getPacketTrafficHandler().registerListener(this,
        this::onPacketMetricMonitoring);
    this.getRouter().getMetric().getChannelTrafficHandler().registerListener(this,
        this::onTrafficMetricMonitoring);

    this.getRouter().getEventBus().register(pushServiceManager);

  }

  @Override
  protected void onDisable() throws Exception {

    this.getRouter().getEventBus().unregister(pushServiceManager);

    this.getRouter().getMetric().getPacketTrafficHandler().unregisterListeners(this);
    this.getRouter().getMetric().getChannelTrafficHandler().unregisterListeners(this);

  }

  private void onPacketMetricMonitoring() {

  }

  private void onTrafficMetricMonitoring() {

  }

}
