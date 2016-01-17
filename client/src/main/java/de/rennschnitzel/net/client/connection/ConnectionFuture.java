package de.rennschnitzel.net.client.connection;

import java.util.concurrent.Future;

import de.rennschnitzel.net.core.Connection;

public interface ConnectionFuture<C extends Connection> extends Future<C> {

  boolean isActive();

}
