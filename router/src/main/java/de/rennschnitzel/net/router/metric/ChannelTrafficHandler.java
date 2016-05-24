package de.rennschnitzel.net.router.metric;

import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.LongAdder;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.Getter;

public class ChannelTrafficHandler extends ObjectMonitorChannelDuplexHandler<ByteBuf> {

  public static interface ByteCount {
    long getTimestamp();
    
    long getLastBytesRead();

    long getLastBytesWrite();
  }

  private static class ByteCounter implements ByteCount {
    private final LongAdder write = new LongAdder();
    private final LongAdder read = new LongAdder();
    private @Getter volatile long timestamp = 0;
    private @Getter volatile long lastBytesWrite = 0;
    private @Getter volatile long lastBytesRead = 0;

    private void sumThenReset() {
      lastBytesWrite = write.sumThenReset();
      lastBytesRead = read.sumThenReset();
      timestamp = System.currentTimeMillis();
    }

    public String toString() {
      return "Bytes Write: " + this.lastBytesWrite + ", Read: " + this.lastBytesRead;
    }
  }


  private static class GlobalByteCount implements ByteCount {
    private @Getter volatile long timestamp = 0;
    private @Getter volatile long lastBytesWrite = 0;
    private @Getter volatile long lastBytesRead = 0;

    private void update(Collection<ByteCounter> c) {
      lastBytesWrite = c.stream().mapToLong(p -> p.lastBytesWrite).sum();
      lastBytesRead = c.stream().mapToLong(p -> p.lastBytesRead).sum();
      timestamp = System.currentTimeMillis();
    }

    public String toString() {
      return "Bytes Write: " + this.lastBytesWrite + ", Read: " + this.lastBytesRead;
    }
  }


  // *****************************

  private final GlobalByteCount globalCount = new GlobalByteCount();
  private final LoadingCache<Channel, ByteCounter> counter =
      CacheBuilder.newBuilder().weakKeys().build(CacheLoader.from(() -> new ByteCounter()));

  private @Getter final boolean sharable = true;
  
  public ChannelTrafficHandler(ScheduledExecutorService executor) {
    super(ByteBuf.class, executor);
  }

  public ChannelTrafficHandler(ScheduledExecutorService executor, long checkInterval) {
    this(executor);
    this.setCheckIntervall(checkInterval);
  }

  public ByteCount getGlobal() {
    return this.globalCount;
  }

  public ByteCount get(Channel channel) {
    return counter.getIfPresent(channel);
  }

  @Override
  protected void onWrite(ChannelHandlerContext ctx, ByteBuf msg, ChannelPromise promise)
      throws Exception {
    counter.get(ctx.channel()).write.add(msg.readableBytes());
  }

  @Override
  protected void onRead(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    counter.get(ctx.channel()).read.add(msg.readableBytes());
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
