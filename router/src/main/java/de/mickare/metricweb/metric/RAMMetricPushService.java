package de.mickare.metricweb.metric;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.ImmutableList;

import de.mickare.metricweb.MetricWebPlugin;
import de.mickare.metricweb.PushService;
import de.mickare.metricweb.protocol.Packet;
import de.mickare.metricweb.protocol.WebProtocol.PacketData;
import de.mickare.metricweb.websocket.WebConnection;
import lombok.AllArgsConstructor;
import lombok.Data;

public class RAMMetricPushService extends PushService implements Stoppable {

  private final MetricWebPlugin plugin;

  private final List<RAMMetric> history = new EvictingLinkedList<>(100);

  private final AtomicReference<RAMMetric> lastMetric = new AtomicReference<>(new RAMMetric());

  private volatile boolean monitoring = false;
  private ScheduledFuture<?> scheduledFuture = null;

  public RAMMetricPushService(MetricWebPlugin plugin) {
    super("ramMetric");
    this.plugin = plugin;
  }

  public RAMMetric getLastMetric() {
    return lastMetric.get();
  }

  public void register() {
    plugin.getPushServiceManager().register(this);
  }

  private class MonitoringTask implements Runnable {
    @Override
    public void run() {
      if (!monitoring) {
        return;
      }
      doMetric();
    }
  }

  public synchronized void start() {
    if (monitoring) {
      return;
    }
    this.scheduledFuture = plugin.getRouter().getScheduler()
        .scheduleWithFixedDelay(new MonitoringTask(), 1, 1, TimeUnit.SECONDS);
    monitoring = true;
  }

  public synchronized void stop() {
    if (!monitoring) {
      return;
    }
    if (scheduledFuture != null) {
      scheduledFuture.cancel(true);
    }
    monitoring = false;
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

  private void doMetric() {
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
