package de.mickare.metricweb;

import java.io.File;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Sets;

import de.mickare.metricweb.metric.CPUMetricPushService;
import de.mickare.metricweb.metric.PacketMetricPushService;
import de.mickare.metricweb.metric.RAMMetricPushService;
import de.mickare.metricweb.metric.Stoppable;
import de.mickare.metricweb.metric.TrafficMetricPushService;
import de.mickare.metricweb.protocol.RouterWebProtocol;
import de.mickare.metricweb.websocket.WebSocketServer;
import de.rennschnitzel.net.router.plugin.Plugin;
import de.rennschnitzel.net.router.plugin.RouterPlugin;
import lombok.Getter;

@RouterPlugin(name = "MetricWeb", version = "0.0.1", author = "mickare")
public class MetricWebPlugin extends Plugin {

  private @Getter ServiceManager pushServiceManager;
  private @Getter WebSocketServer socketServer;

  private Set<Stoppable> stoppable = Sets.newHashSet();

  @Override
  protected void onLoad() throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  protected void onEnable() throws Exception {

    pushServiceManager = new ServiceManager(this);

    this.getRouter().getEventBus().register(pushServiceManager);

    this.socketServer = new WebSocketServer(getLogger(), getRouter().getEventLoop(), 5546, false,
        new RouterWebProtocol());
    this.socketServer.startAsync();

    pushServiceManager.register(new PacketMetricPushService(this));
    pushServiceManager.register(new TrafficMetricPushService(this));

    CPUMetricPushService cpu = new CPUMetricPushService(this);
    stoppable.add(cpu);
    cpu.start();
    pushServiceManager.register(cpu);

    RAMMetricPushService ram = new RAMMetricPushService(this);
    stoppable.add(ram);
    ram.start();
    pushServiceManager.register(ram);

    try {
      this.socketServer.awaitRunning(5, TimeUnit.SECONDS);
    } catch (Exception e) {
      this.socketServer.stopAsync();
      throw e;
    }

    if (!this.socketServer.isRunning()) {
      throw new IllegalStateException("WebSocket Server not running!");
    }

    File target = new File(System.getProperty("user.dir"), "html");
    if (!target.isDirectory() && target.mkdir()) {
      FileUtils.copyResourcesRecursively(this.getClass().getResource("/html"), target);
    }

  }



  @Override
  protected void onDisable() throws Exception {

    this.socketServer.stopAsync();

    this.stoppable.forEach(s -> s.stop());
    this.stoppable.clear();

    this.getRouter().getEventBus().unregister(pushServiceManager);

    this.getRouter().getMetric().getPacketTrafficHandler().unregisterListeners(this);
    this.getRouter().getMetric().getChannelTrafficHandler().unregisterListeners(this);

  }

}
