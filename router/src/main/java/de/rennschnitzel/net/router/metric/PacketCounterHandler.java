package de.rennschnitzel.net.router.metric;

import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.Getter;

public class PacketCounterHandler<T> extends ChannelDuplexHandler {

  public static interface PacketCount {
    long getLastPacketsWrite();

    long getLastPacketsRead();
  }

  private static class PacketCounter implements PacketCount {
    private final LongAdder write = new LongAdder();
    private final LongAdder read = new LongAdder();
    private @Getter volatile long lastPacketsWrite = 0;
    private @Getter volatile long lastPacketsRead = 0;

    private void sumThenReset() {
      lastPacketsWrite = write.sumThenReset();
      lastPacketsRead = read.sumThenReset();
    }
  }

  private static class TotalCount implements PacketCount {
    private @Getter volatile long lastPacketsWrite = 0;
    private @Getter volatile long lastPacketsRead = 0;

    private void update(Collection<PacketCounter> c) {
      lastPacketsWrite = c.stream().mapToLong(p -> p.lastPacketsWrite).sum();
      lastPacketsRead = c.stream().mapToLong(p -> p.lastPacketsRead).sum();
    }
  }


  private volatile boolean monitorActive = false;
  private Runnable monitor;
  private @Getter final boolean sharable = true;
  private @Getter final Class<T> type;
  private final ScheduledExecutorService executor;
  private final AtomicLong checkInterval = new AtomicLong(1000);
  private volatile ScheduledFuture<?> scheduledFuture = null;

  private @Getter final TotalCount totalCount = new TotalCount();
  private final LoadingCache<Channel, PacketCounter> counter =
      CacheBuilder.newBuilder().weakKeys().build(CacheLoader.from(() -> new PacketCounter()));

  public PacketCounterHandler(ScheduledExecutorService executor, Class<T> type,
      long checkInterval) {
    Preconditions.checkNotNull(executor);
    Preconditions.checkNotNull(type);
    this.setCheckIntervall(checkInterval);
    this.type = type;
    this.executor = executor;

    start();
  }

  public void setCheckIntervall(long checkInterval) {
    Preconditions.checkArgument(checkInterval > 0);
    this.checkInterval.set(checkInterval);
  }

  private final class MonitoringTask implements Runnable {
    @Override
    public void run() {
      if (!monitorActive) {
        return;
      }
      resetAccounting();
      scheduledFuture = executor.schedule(this, checkInterval.get(), TimeUnit.MILLISECONDS);
    }
  }

  private void resetAccounting() {
    counter.asMap().values().forEach(c -> c.sumThenReset());
    totalCount.update(counter.asMap().values());
  }

  public synchronized void start() {
    if (monitorActive) {
      return;
    }
    monitorActive = true;
    monitor = new MonitoringTask();
    scheduledFuture = executor.schedule(monitor, checkInterval.get(), TimeUnit.MILLISECONDS);
  }

  public synchronized void stop() {
    if (!monitorActive) {
      return;
    }
    monitorActive = false;
    if (scheduledFuture != null) {
      scheduledFuture.cancel(true);
    }
  }

  public PacketCount getCount(Channel channel) {
    return counter.getIfPresent(channel);
  }

  public boolean isApplicable(Object msg) {
    return type.isInstance(msg);
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    if (this.isApplicable(msg)) {
      counter.get(ctx.channel()).write.increment();
    }
    super.write(ctx, msg, promise);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (this.isApplicable(msg)) {
      counter.get(ctx.channel()).read.increment();
    }
    super.channelRead(ctx, msg);
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    counter.refresh(ctx.channel());
    super.handlerAdded(ctx);
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    counter.invalidate(ctx.channel());
    super.handlerRemoved(ctx);
  }


}
