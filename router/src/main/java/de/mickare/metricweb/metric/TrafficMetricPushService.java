package de.mickare.metricweb.metric;

import java.util.List;

import com.google.common.collect.ImmutableList;

import de.mickare.metricweb.MetricWebPlugin;
import de.mickare.metricweb.PushService;
import de.mickare.metricweb.protocol.Packet;
import de.mickare.metricweb.protocol.WebProtocol.PacketData;
import de.mickare.metricweb.websocket.WebConnection;
import io.netty.handler.traffic.CustomGlobalChannelTrafficShapingHandler;
import io.netty.handler.traffic.TrafficCounter;
import lombok.AllArgsConstructor;
import lombok.Data;

public class TrafficMetricPushService extends PushService {

  private final List<TrafficMetric> history = new EvictingLinkedList<>(100);

  public TrafficMetricPushService(MetricWebPlugin plugin) {
    super("trafficMetric");
    plugin.getRouter().getMetric().getChannelTrafficHandler().registerListener(plugin,
        this::onAccounting);
  }

  @Override
  protected void onSubscribe(WebConnection con) throws Exception {
    List<TrafficMetric> temp;
    synchronized (history) {
      temp = ImmutableList.copyOf(this.history);
    }
    temp.forEach(p -> con.sendFast(p));
    con.flush();
  }

  @Override
  protected void onUnsubscribe(WebConnection con) throws Exception {}

  @Packet(name = "trafficMetric")
  public static @Data @AllArgsConstructor class TrafficMetric implements PacketData {
    private String channel;
    private long timestamp;
    private long writeThroughput, readThroughput;
    private long writtenBytes, readBytes;

    public TrafficMetric(String channel, TrafficCounter counter) {
      this.channel = channel;
      this.timestamp = counter.lastTime();
      this.writeThroughput = counter.lastWriteThroughput();
      this.readThroughput = counter.lastReadThroughput();
      this.writtenBytes = counter.lastWrittenBytes();
      this.readBytes = counter.lastReadBytes();
    }

  }

  protected void onAccounting(CustomGlobalChannelTrafficShapingHandler handler) {
    TrafficCounter global = handler.trafficCounter();
    TrafficMetric packet = new TrafficMetric(null, global);
    synchronized (history) {
      this.history.add(packet);
    }
    this.pushAndFlush(packet);
  }

}
