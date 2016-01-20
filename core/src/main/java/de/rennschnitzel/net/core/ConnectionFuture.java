package de.rennschnitzel.net.core;

import com.google.common.util.concurrent.ListenableFuture;

import de.rennschnitzel.net.core.Connection;

public interface ConnectionFuture<C extends Connection> extends ListenableFuture<C> {

  boolean isOpen();

}
