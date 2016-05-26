package io.netty.handler.traffic;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

import com.google.common.base.Preconditions;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

public class CustomGlobalChannelTrafficShapingHandler extends GlobalChannelTrafficShapingHandler {

  private ConcurrentHashMap<Consumer<CustomGlobalChannelTrafficShapingHandler>, Object> monitorListeners =
      new ConcurrentHashMap<>();

  public CustomGlobalChannelTrafficShapingHandler(ScheduledExecutorService executor) {
    super(executor);
  }

  public CustomGlobalChannelTrafficShapingHandler(ScheduledExecutorService executor,
      long checkInterval) {
    super(executor, checkInterval);
  }

  public CustomGlobalChannelTrafficShapingHandler(ScheduledExecutorService executor,
      long writeGlobalLimit, long readGlobalLimit, long writeChannelLimit, long readChannelLimit) {
    super(executor, writeGlobalLimit, readGlobalLimit, writeChannelLimit, readChannelLimit);
  }

  public CustomGlobalChannelTrafficShapingHandler(ScheduledExecutorService executor,
      long writeGlobalLimit, long readGlobalLimit, long writeChannelLimit, long readChannelLimit,
      long checkInterval) {
    super(executor, writeGlobalLimit, readGlobalLimit, writeChannelLimit, readChannelLimit,
        checkInterval);
  }

  public CustomGlobalChannelTrafficShapingHandler(ScheduledExecutorService executor,
      long writeGlobalLimit, long readGlobalLimit, long writeChannelLimit, long readChannelLimit,
      long checkInterval, long maxTime) {
    super(executor, writeGlobalLimit, readGlobalLimit, writeChannelLimit, readChannelLimit,
        checkInterval, maxTime);
  }

  public TrafficCounter getTrafficCounter(ChannelHandlerContext ctx) {
    return getTrafficCounter(ctx.channel());
  }

  public TrafficCounter getTrafficCounter(Channel channel) {
    PerChannel perChannel = this.channelQueues.get(channel.hashCode());
    if (perChannel != null) {
      return perChannel.channelTrafficCounter;
    }
    return null;
  }

  @Override
  protected void doAccounting(TrafficCounter counter) {
    super.doAccounting(counter);
    callListeners();
  }

  public void registerListener(Object owner,
      Consumer<CustomGlobalChannelTrafficShapingHandler> listener) {
    Preconditions.checkNotNull(owner);
    Preconditions.checkNotNull(listener);
    this.monitorListeners.put(listener, owner);
  }

  public boolean unregisterListener(Consumer<CustomGlobalChannelTrafficShapingHandler> listener) {
    Preconditions.checkNotNull(listener);
    return this.monitorListeners.remove(listener) != null;
  }

  public boolean unregisterListeners(Object owner) {
    Preconditions.checkNotNull(owner);
    return this.monitorListeners.entrySet().removeIf(e -> e.getValue().equals(owner));
  }

  private void callListeners() {
    this.monitorListeners.forEachKey(1, l -> l.accept(this));
  }

}
