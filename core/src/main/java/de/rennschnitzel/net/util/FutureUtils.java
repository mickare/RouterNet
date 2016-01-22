package de.rennschnitzel.net.util;

import java.util.function.Consumer;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.Future;

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

  public static <V> void onSuccess(final ListenableFuture<V> future, final Consumer<V> callback) {
    Futures.addCallback(future, new FutureCallback<V>() {
      public void onFailure(Throwable cause) {}

      public void onSuccess(V value) {
        callback.accept(value);
      }
    });
  }

  public static void onFailure(final ListenableFuture<?> future, final Consumer<Throwable> callback) {
    Futures.addCallback(future, new FutureCallback<Object>() {
      public void onFailure(Throwable cause) {
        callback.accept(cause);
      }

      public void onSuccess(Object value) {}
    });
  }

}
