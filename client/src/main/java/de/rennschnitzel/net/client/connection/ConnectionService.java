package de.rennschnitzel.net.client.connection;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Service;

import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.exception.NotConnectedException;

public interface ConnectionService extends Service {

  Connection getConnection(long time, TimeUnit unit) throws NotConnectedException;

  ListenableFuture<? extends Connection> getConnectionFuture();

  default void onConnected(final Consumer<Connection> consumer) {
    Futures.addCallback(getConnectionFuture(), new FutureCallback<Connection>() {
      @Override
      public void onFailure(Throwable arg0) {}

      @Override
      public void onSuccess(Connection con) {
        consumer.accept(con);
      }
    });
  }

}
