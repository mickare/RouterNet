package de.mickare.metricweb.metric;

import java.util.List;

import com.google.common.collect.ImmutableList;

import de.mickare.metricweb.MetricWebPlugin;
import de.mickare.metricweb.PushService;
import de.mickare.metricweb.protocol.Packet;
import de.mickare.metricweb.protocol.WebProtocol.PacketData;
import de.mickare.metricweb.websocket.WebConnection;
import de.rennschnitzel.net.router.metric.PacketTrafficHandler.PacketCount;
import lombok.AllArgsConstructor;
import lombok.Data;

public class PacketMetricPushService extends PushService {

  private final MetricWebPlugin plugin;

  private final List<PacketMetric> history = new EvictingLinkedList<>(100);

  public PacketMetricPushService(MetricWebPlugin plugin) {
    super("packetMetric");
    this.plugin = plugin;
  }

  public void register() {
    plugin.getPushServiceManager().register(this);
    plugin.getRouter().getMetric().getPacketTrafficHandler().registerListener(plugin,
        this::onMetric);
  }

  @Override
  protected void onSubscribe(WebConnection con) throws Exception {
    List<PacketMetric> temp;
    synchronized (this) {
      temp = ImmutableList.copyOf(history);
    }
    temp.forEach(p -> con.sendFast(p));
  }

  @Override
  protected void onUnsubscribe(WebConnection con) throws Exception {}

  private void onMetric() {
    PacketCount count = plugin.getRouter().getMetric().getPacketTrafficHandler().getGlobal();
    PacketMetric packet = new PacketMetric(count);
    synchronized (this) {
      this.history.add(packet);
    }
    this.pushAndFlush(packet);
  }


  @Packet(name = "packetMetric")
  public static @Data @AllArgsConstructor class PacketMetric implements PacketData {
    private long timestamp;
    private long write;
    private long read;

    public PacketMetric(PacketCount count) {
      this.timestamp = count.getTimestamp();
      this.write = (long) (count.getLastPacketsWrite() + Math.random() * 10);
      this.read = (long) (count.getLastPacketsRead() + Math.random() * 10);
    }

  }

}
