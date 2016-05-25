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
import de.mickare.metricweb.protocol.MetricWebProtocol;
import de.mickare.metricweb.websocket.WebSocketServer;
import de.rennschnitzel.net.router.plugin.JavaPlugin;
import de.rennschnitzel.net.router.plugin.Plugin;
import lombok.Getter;

@Plugin(name = "MetricWeb", version = "0.0.1", author = "mickare")
public class MetricWebPlugin extends JavaPlugin {

  private @Getter PushServiceManager pushServiceManager;
  private @Getter WebSocketServer socketServer;

  private Set<Stoppable> stoppable = Sets.newHashSet();
  
  @Override
  protected void onLoad() throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  protected void onEnable() throws Exception {

    pushServiceManager = new PushServiceManager(this);

    this.getRouter().getEventBus().register(pushServiceManager);

    this.socketServer = new WebSocketServer(getLogger(), getRouter().getEventLoop(), 5546, false, new MetricWebProtocol());
    this.socketServer.startAsync();

    new PacketMetricPushService(this).register();
    new TrafficMetricPushService(this).register();

    CPUMetricPushService cpu = new CPUMetricPushService(this);
    stoppable.add(cpu);
    cpu.start();
    cpu.register();
    
    RAMMetricPushService ram = new RAMMetricPushService(this);
    stoppable.add(ram);
    ram.start();
    ram.register();
    
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
