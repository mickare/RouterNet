package de.rennschnitzel.backbone.util;

import java.util.function.Consumer;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class FutureUtils {

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
