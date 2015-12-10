package de.rennschnitzel.backbone.util;

import com.google.common.util.concurrent.UncheckedExecutionException;

import de.rennschnitzel.backbone.util.function.CheckedSupplier;

public class LazyCache<T> implements CheckedSupplier<T> {

  public static <T> LazyCache<T> of(CheckedSupplier<T> sup) {
    return new LazyCache<T>(sup);
  }

  private final CheckedSupplier<T> supplier;

  private volatile CheckedSupplier<T> cache;

  public LazyCache(CheckedSupplier<T> supplier) {
    this.supplier = supplier;
    reset();
  }

  private void reset() {
    cache = new MemoizingSupplier<T>(supplier);
  }

  public T getUnchecked() throws UncheckedExecutionException {
    try {
      return get();
    } catch (Exception e) {
      throw new UncheckedExecutionException(e);
    }
  }

  @Override
  public T get() throws Exception {
    return cache.get();
  }

  public void invalidate() {
    reset();
  }

  private static class MemoizingSupplier<T> implements CheckedSupplier<T> {
    final CheckedSupplier<T> delegate;
    volatile T value;

    MemoizingSupplier(CheckedSupplier<T> delegate) {
      this.delegate = delegate;
    }

    @Override
    public T get() throws Exception {
      if (value == null) {
        synchronized (this) {
          if (value == null) {
            value = delegate.get();
          }
        }
      }
      return value;
    }
  }
}
