package de.mickare.net.metric;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.Preconditions;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.Getter;

public abstract class ObjectMonitorChannelDuplexHandler<T> extends ChannelDuplexHandler {


  private @Getter final Class<T> objectType;
  private @Getter volatile boolean monitorActive = false;
  private Runnable monitor;
  private final ScheduledExecutorService executor;
  private final AtomicLong checkInterval = new AtomicLong(1000);
  private volatile ScheduledFuture<?> scheduledFuture = null;

  private ConcurrentHashMap<Runnable, Object> monitorListeners = new ConcurrentHashMap<>();

  public ObjectMonitorChannelDuplexHandler(Class<T> objectType, ScheduledExecutorService executor) {
    Preconditions.checkNotNull(objectType);
    Preconditions.checkNotNull(executor);
    this.objectType = objectType;
    this.executor = executor;
    start();
  }

  public void registerListener(Object owner, Runnable listener) {
    Preconditions.checkNotNull(owner);
    Preconditions.checkNotNull(listener);
    this.monitorListeners.put(listener, owner);
  }

  public boolean unregisterListener(Runnable listener) {
    Preconditions.checkNotNull(listener);
    return this.monitorListeners.remove(listener) != null;
  }

  public boolean unregisterListeners(Object owner) {
    Preconditions.checkNotNull(owner);
    return this.monitorListeners.entrySet().removeIf(e -> e.getValue().equals(owner));
  }

  private void callListeners() {
    this.monitorListeners.forEachKey(1, l -> l.run());
  }

  public void setCheckIntervall(long checkInterval) {
    Preconditions.checkArgument(checkInterval > 0);
    this.checkInterval.set(checkInterval);
  }

  public boolean isApplicable(Object msg) {
    return objectType.isInstance(msg);
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    if (this.isApplicable(msg)) {
      onWrite(ctx, objectType.cast(msg), promise);
    }
    super.write(ctx, msg, promise);
  }

  protected abstract void onWrite(ChannelHandlerContext ctx, T msg, ChannelPromise promise)
      throws Exception;

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (this.isApplicable(msg)) {
      onRead(ctx, objectType.cast(msg));
    }
    super.channelRead(ctx, msg);
  }

  protected abstract void onRead(ChannelHandlerContext ctx, T msg) throws Exception;


  private final class MonitoringTask implements Runnable {
    @Override
    public void run() {
      if (!monitorActive) {
        return;
      }
      long start = System.currentTimeMillis();
      doMonitoring();
      callListeners();
      long diff = System.currentTimeMillis() - start;
      long intervall = checkInterval.get() - diff;
      scheduledFuture =
          executor.schedule(this, intervall > 0 ? intervall : 1, TimeUnit.MILLISECONDS);
    }
  }

  protected abstract void doMonitoring();

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

}
