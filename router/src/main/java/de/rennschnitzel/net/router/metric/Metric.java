package de.rennschnitzel.net.router.metric;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class Metric {

  private final ConcurrentHashMap<String, LongAdder> longMetrics = new ConcurrentHashMap<>();

  private LongAdder getLongAdder(String key) {
    return longMetrics.computeIfAbsent(key, k -> new LongAdder());
  }

  public void addLong(String key, long value) {
    getLongAdder(key).add(value);
  }

  public long getLong(String key) {
    return getLongAdder(key).longValue();
  }

  public void reset() {
    longMetrics.forEachValue(1, LongAdder::reset);
  }


}
