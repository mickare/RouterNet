package de.rennschnitzel.net.core;

import io.netty.util.concurrent.Future;

public interface ConnectionSupplier {

  Future<Connection> getConnectionPromise();

  boolean isDone();

  boolean isSuccess();

  boolean isChannelActive();

  Future<?> tryDisconnect(String reason);

}
