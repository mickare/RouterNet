package de.rennschnitzel.backbone.util.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public class DirectScheduledExecutorService extends DirectExecutorService implements ScheduledExecutorService {

  private static <V> ForwardingScheduledFuture<V> of(Future<V> future, long delayMillis) {
    if (future == null) {
      return null;
    }
    return new ForwardingScheduledFuture<>(future, delayMillis);
  }

  private static boolean warned = false;

  private static void warn() {
    if (!warned) {
      warned = true;
      System.out.println("Waring! Don't use this scheduler, only if you know what you are doing!");
      Thread.dumpStack();
    }
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    warn();
    return of(submit(command), unit.toMillis(delay));
  }

  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
    warn();
    return of(submit(callable), unit.toMillis(delay));
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
    warn();
    return of(submit(command), unit.toMillis(initialDelay));
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
    warn();
    return of(submit(command), unit.toMillis(initialDelay));
  }

  @RequiredArgsConstructor
  private static class ForwardingScheduledFuture<V> implements ScheduledFuture<V> {

    @NonNull
    private final Future<V> delegate;

    private final long delayMillis;

    @Override
    public long getDelay(TimeUnit unit) {
      return unit.convert(delayMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
      return Long.compare(this.delayMillis, o.getDelay(TimeUnit.MILLISECONDS));
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      return delegate.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
      return delegate.isCancelled();
    }

    @Override
    public boolean isDone() {
      return delegate.isDone();
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
      return delegate.get();
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      return delegate.get(timeout, unit);
    }
  }

}
