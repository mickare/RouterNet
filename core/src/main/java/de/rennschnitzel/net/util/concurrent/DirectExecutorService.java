package de.rennschnitzel.net.util.concurrent;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;

import lombok.Getter;

public class DirectExecutorService implements ExecutorService {

  private @Getter volatile boolean shutdown = false;
  protected final CloseableReadWriteLock lock = new ReentrantCloseableReadWriteLock();

  @Override
  public void shutdown() {
    shutdown = true;
  }

  @Override
  public List<Runnable> shutdownNow() {
    shutdown();
    return Lists.newArrayList();
  }

  @Override
  public boolean isTerminated() {
    return shutdown;
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    if (lock.writeLock().tryLock(timeout, unit)) {
      lock.writeLock().unlock();
    }
    return isTerminated();
  }


  @Override
  public void execute(Runnable command) {
    if (isShutdown()) {
      throw new RejectedExecutionException();
    }
    command.run();
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    try (CloseableLock l = lock.readLock().open()) {
      FutureTask<T> f = new FutureTask<>(task);
      execute(f);
      return f;
    }
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    try (CloseableLock l = lock.readLock().open()) {
      FutureTask<T> f = new FutureTask<>(task, result);
      execute(f);
      return f;
    }
  }

  @Override
  public Future<?> submit(Runnable task) {
    try (CloseableLock l = lock.readLock().open()) {
      FutureTask<?> f = new FutureTask<>(task, null);
      execute(f);
      return f;
    }
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
    try (CloseableLock l = lock.readLock().open()) {
      List<Future<T>> result = Lists.newArrayListWithExpectedSize(tasks.size());
      for (Callable<T> task : tasks) {
        result.add(submit(task));
      }
      return result;
    }
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
    Preconditions.checkArgument(timeout > 0);
    try (CloseableLock l = lock.readLock().open()) {
      long timeFinish = System.currentTimeMillis() + unit.toMillis(timeout);
      List<Future<T>> result = Lists.newArrayListWithExpectedSize(tasks.size());
      for (Callable<T> task : tasks) {
        if (System.currentTimeMillis() < timeFinish) {
          result.add(submit(task));
        } else {
          result.add(Futures.immediateCancelledFuture());
        }
      }
      return result;
    }
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    if (isShutdown()) {
      throw new RejectedExecutionException();
    }
    try (CloseableLock l = lock.readLock().open()) {
      for (Callable<T> task : tasks) {
        try {
          return submit(task).get();
        } catch (CancellationException | ExecutionException e) {
        }
      }
    }
    throw new ExecutionException(new RuntimeException());
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    Preconditions.checkArgument(timeout > 0);
    if (isShutdown()) {
      throw new RejectedExecutionException();
    }
    try (CloseableLock l = lock.readLock().open()) {
      long timeFinish = System.currentTimeMillis() + unit.toMillis(timeout);
      for (Callable<T> task : tasks) {
        if (System.currentTimeMillis() < timeFinish) {
          throw new TimeoutException();
        }
        try {
          return submit(task).get();
        } catch (CancellationException | ExecutionException e) {
        }
      }
    }
    throw new ExecutionException(new RuntimeException());
  }


}
