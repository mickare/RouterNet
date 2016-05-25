package de.mickare.metricweb.metric;

import java.util.List;

import com.google.common.collect.ImmutableList;

import de.mickare.metricweb.MetricWebPlugin;
import de.mickare.metricweb.PushService;
import de.mickare.metricweb.protocol.Packet;
import de.mickare.metricweb.protocol.WebProtocol.PacketData;
import de.mickare.metricweb.websocket.WebConnection;
import de.rennschnitzel.net.router.metric.ChannelTrafficHandler.ByteCount;
import lombok.AllArgsConstructor;
import lombok.Data;

public class TrafficMetricPushService extends PushService {

  private final MetricWebPlugin plugin;

  private final List<ByteMetric> history = new EvictingLinkedList<>(100);

  public TrafficMetricPushService(MetricWebPlugin plugin) {
    super("trafficMetric");
    this.plugin = plugin;
  }

  public void register() {
    plugin.getPushServiceManager().register(this);
    plugin.getRouter().getMetric().getChannelTrafficHandler().registerListener(plugin,
        this::onMetric);
  }

  @Override
  protected void onSubscribe(WebConnection con) throws Exception {
    List<ByteMetric> temp;
    synchronized (history) {
      temp = ImmutableList.copyOf(this.history);
    }
    temp.forEach(p -> con.sendFast(p));
    con.flush();
  }

  @Override
  protected void onUnsubscribe(WebConnection con) throws Exception {}

  private void onMetric() {
    ByteCount count = plugin.getRouter().getMetric().getChannelTrafficHandler().getGlobal();
    ByteMetric packet = new ByteMetric(count);
    synchronized (history) {
      this.history.add(packet);
    }
    this.pushAndFlush(packet);
  }


  @Packet(name = "trafficMetric")
  public static @Data @AllArgsConstructor class ByteMetric implements PacketData {
    private long timestamp;
    private long write;
    private long read;

    public ByteMetric(ByteCount count) {
      this.timestamp = count.getTimestamp();
      this.write = (long) (count.getLastBytesWrite()+ Math.random() * 10);
      this.read = (long) (count.getLastBytesRead()+ Math.random() * 10);
    }

  }

}
