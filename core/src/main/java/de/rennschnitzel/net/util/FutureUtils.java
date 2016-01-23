package de.rennschnitzel.net.util;

import java.util.function.Function;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import de.rennschnitzel.net.util.function.CheckedConsumer;
import de.rennschnitzel.net.util.function.CheckedFunction;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;

public class FutureUtils {

  public static final Future<Void> SUCCESS = futureSuccess(null);

  public static <V> Future<V> futureSuccess(V value) {
    return ImmediateEventExecutor.INSTANCE.<V>newSucceededFuture(value);
  }

  public static <V> Future<V> futureFailure(Throwable t) {
    return ImmediateEventExecutor.INSTANCE.<V>newFailedFuture(t);
  }

  public static <V> Future<V> transformFuture(final ListenableFuture<V> future) {
    final Promise<V> promise = ImmediateEventExecutor.INSTANCE.newPromise();
    Futures.addCallback(future, new FutureCallback<V>() {
      @Override
      public void onSuccess(V result) {
        promise.setSuccess(result);
      }

      @Override
      public void onFailure(Throwable t) {
        promise.setFailure(t);
      }
    });
    return promise;
  }

  public static <V, F extends Future<V>> void on(F future, final CheckedConsumer<Future<V>> callback) {
    future.addListener(new FutureListener<V>() {
      public void operationComplete(Future<V> future) throws Exception {
        callback.accept(future);
      }
    });
  }

  public static <T, R> Future<R> combine(Future<T> future, CheckedFunction<T, Future<R>> func) {
    Preconditions.checkNotNull(future);
    Preconditions.checkNotNull(func);
    final Promise<R> promise = ImmediateEventExecutor.INSTANCE.newPromise();
    if (future.isSuccess()) {
      try {
        on(func.apply(future.get()), f -> {
          if (f.isSuccess()) {
            promise.trySuccess(f.get());
          } else {
            promise.tryFailure(f.cause());
          }
        });
      } catch (Exception e) {
        promise.tryFailure(e);
      }
    } else {
      promise.tryFailure(future.cause());
    }
    return promise;
  }

  public static <T, R> Future<R> lazyTransform(Future<T> future, Function<T, R> convert) {
    final Promise<R> promise = ImmediateEventExecutor.INSTANCE.newPromise();
    on(future, t -> {
      if (t.isSuccess()) {
        try {
          promise.setSuccess(convert.apply(t.get()));
        } catch (Exception e) {
          promise.setFailure(e);
        }
      } else {
        promise.setFailure(t.cause());
      }
    });
    return promise;
  }

}
