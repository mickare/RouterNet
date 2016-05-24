package de.mickare.metricweb;

import java.util.concurrent.TimeUnit;

import de.mickare.metricweb.metric.PacketMetricPushService;
import de.mickare.metricweb.metric.TrafficMetricPushService;
import de.mickare.metricweb.protocol.MetricWebProtocol;
import de.mickare.metricweb.websocket.WebSocketServer;
import de.rennschnitzel.net.router.plugin.JavaPlugin;
import de.rennschnitzel.net.router.plugin.Plugin;
import lombok.Getter;

@Plugin(name = "MetricWeb", version = "0.0.1", author = "mickare")
public class MetricWebPlugin extends JavaPlugin {

  private @Getter PushServiceManager pushServiceManager;
  private @Getter WebSocketServer socketServer;

  @Override
  protected void onLoad() throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  protected void onEnable() throws Exception {

    pushServiceManager = new PushServiceManager(this);

    this.getRouter().getEventBus().register(pushServiceManager);

    this.socketServer = new WebSocketServer(getLogger(), 5546, false, new MetricWebProtocol());
    this.socketServer.startAsync();

    new PacketMetricPushService(this).register();
    new TrafficMetricPushService(this).register();

    try {
      this.socketServer.awaitRunning(5, TimeUnit.SECONDS);
    } catch (Exception e) {
      this.socketServer.stopAsync();
      throw e;
    }

    if (!this.socketServer.isRunning()) {
      throw new IllegalStateException("WebSocket Server not running!");
    }
  }

  @Override
  protected void onDisable() throws Exception {

    this.socketServer.stopAsync();

    this.getRouter().getEventBus().unregister(pushServiceManager);

    this.getRouter().getMetric().getPacketTrafficHandler().unregisterListeners(this);
    this.getRouter().getMetric().getChannelTrafficHandler().unregisterListeners(this);

  }

}
