package de.mickare.metricweb.metric;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.ImmutableList;

import de.mickare.metricweb.MetricWebPlugin;
import de.mickare.metricweb.protocol.Packet;
import de.mickare.metricweb.protocol.WebProtocol.PacketData;
import de.mickare.metricweb.websocket.WebConnection;
import lombok.AllArgsConstructor;
import lombok.Data;

public class RAMMetricPushService extends TimedPushService {

  private final List<RAMMetric> history = new EvictingLinkedList<>(100);

  private final AtomicReference<RAMMetric> lastMetric = new AtomicReference<>(new RAMMetric());

  public RAMMetricPushService(MetricWebPlugin plugin) {
    super(plugin, "ramMetric");
  }

  public RAMMetric getLastMetric() {
    return lastMetric.get();
  }

  @Override
  protected void onSubscribe(WebConnection con) throws Exception {
    List<RAMMetric> temp;
    synchronized (this) {
      temp = ImmutableList.copyOf(history);
    }
    temp.forEach(p -> con.sendFast(p));
    con.flush();
  }

  @Override
  protected void onUnsubscribe(WebConnection con) throws Exception {}

  @Override
  protected void runOneIteration() throws Exception {
    RAMMetric packet = new RAMMetric();
    this.lastMetric.set(packet);
    synchronized (this) {
      this.history.add(packet);
    }
    this.pushAndFlush(packet);
  }


  @Packet(name = "ramMetric")
  public static @Data @AllArgsConstructor class RAMMetric implements PacketData {
    private long timestamp;
    private long maxMemory;
    private long allocatedMemory;
    private long freeMemory;

    public RAMMetric() {
      Runtime runtime = Runtime.getRuntime();
      this.timestamp = System.currentTimeMillis();
      this.maxMemory = runtime.maxMemory();
      this.allocatedMemory = runtime.totalMemory();
      this.freeMemory = runtime.freeMemory();
    }

  }

}
