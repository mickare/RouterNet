package de.mickare.metricweb.metric;

import java.lang.management.ManagementFactory;
import java.util.List;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AtomicDouble;

import de.mickare.metricweb.MetricWebPlugin;
import de.mickare.metricweb.protocol.Packet;
import de.mickare.metricweb.protocol.WebProtocol.PacketData;
import de.mickare.metricweb.websocket.WebConnection;
import lombok.AllArgsConstructor;
import lombok.Data;

public class CPUMetricPushService extends TimedPushService {

  private final List<CPUMetric> history = new EvictingLinkedList<>(100);

  private AtomicDouble cpuLoad = new AtomicDouble(0);

  public CPUMetricPushService(MetricWebPlugin plugin) {
    super(plugin, "cpuMetric");
  }

  public static double getProcessCpuLoad() throws Exception {

    MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
    ObjectName name = ObjectName.getInstance("java.lang:type=OperatingSystem");
    AttributeList list = mbs.getAttributes(name, new String[] {"ProcessCpuLoad"});

    if (list.isEmpty())
      return Double.NaN;

    Attribute att = (Attribute) list.get(0);
    Double value = (Double) att.getValue();

    // usually takes a couple of seconds before we get real values
    if (value == -1.0)
      return Double.NaN;
    // returns a percentage value with 1 decimal point precision
    return ((int) (value * 1000) / 10.0);
  }

  @Override
  protected void onSubscribe(WebConnection con) throws Exception {
    List<CPUMetric> temp;
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
    double load = getProcessCpuLoad();
    if (load == Double.NaN || load < 0) {
      return;
    }
    cpuLoad.set(load);

    CPUMetric packet = new CPUMetric(load);
    synchronized (this) {
      this.history.add(packet);
    }
    this.pushAndFlush(packet);
  }

  @Packet(name = "cpuMetric")
  public static @Data @AllArgsConstructor class CPUMetric implements PacketData {
    private long timestamp;
    private double load;

    public CPUMetric(double load) {
      this.timestamp = System.currentTimeMillis();
      this.load = load;
    }

  }

}
