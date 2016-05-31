package de.rennschnitzel.net.metric;

import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.LongAdder;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.Getter;

public class PacketTrafficHandler<T> extends ObjectMonitorChannelDuplexHandler<T> {

  public static interface PacketCounter {
    long getTimestamp();
    
    long getLastPacketsWrite();

    long getLastPacketsRead();
  }

  private static class SinglePacketCounter implements PacketCounter {
    private final LongAdder write = new LongAdder();
    private final LongAdder read = new LongAdder();
    private @Getter volatile long timestamp = 0;
    private @Getter volatile long lastPacketsWrite = 0;
    private @Getter volatile long lastPacketsRead = 0;

    private void sumThenReset() {
      lastPacketsWrite = write.sumThenReset();
      lastPacketsRead = read.sumThenReset();
      timestamp = System.currentTimeMillis();
    }

    public String toString() {
      return "Packets Write: " + this.lastPacketsWrite + ", Read: " + this.lastPacketsRead;
    }
  }

  private static class GlobalPacketCount implements PacketCounter {
    private @Getter volatile long timestamp = 0;
    private @Getter volatile long lastPacketsWrite = 0;
    private @Getter volatile long lastPacketsRead = 0;

    private void update(Collection<SinglePacketCounter> c) {
      lastPacketsWrite = c.stream().mapToLong(p -> p.lastPacketsWrite).sum();
      lastPacketsRead = c.stream().mapToLong(p -> p.lastPacketsRead).sum();
      timestamp = System.currentTimeMillis();
    }

    public String toString() {
      return "Packets Write: " + this.lastPacketsWrite + ", Read: " + this.lastPacketsRead;
    }
  }


  // *****************************

  private final GlobalPacketCount globalCount = new GlobalPacketCount();
  private final LoadingCache<Channel, SinglePacketCounter> counter =
      CacheBuilder.newBuilder().weakKeys().build(CacheLoader.from(() -> new SinglePacketCounter()));

  private @Getter final boolean sharable = true;

  public PacketTrafficHandler(Class<T> objectType, ScheduledExecutorService executor) {
    super(objectType, executor);
  }

  public PacketTrafficHandler(Class<T> objectType, ScheduledExecutorService executor,
      long checkInterval) {
    super(objectType, executor);
    this.setCheckIntervall(checkInterval);
  }

  public PacketCounter getGlobal() {
    return this.globalCount;
  }

  public PacketCounter get(Channel channel) {
    return counter.getIfPresent(channel);
  }

  @Override
  protected void onWrite(ChannelHandlerContext ctx, T msg, ChannelPromise promise)
      throws Exception {
    counter.get(ctx.channel()).write.increment();
  }

  @Override
  protected void onRead(ChannelHandlerContext ctx, T msg) throws Exception {
    counter.get(ctx.channel()).read.increment();
  }

  @Override
  protected void doMonitoring() {
    counter.asMap().values().forEach(c -> c.sumThenReset());
    globalCount.update(counter.asMap().values());
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
