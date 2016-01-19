package de.rennschnitzel.net.netty;

import com.google.common.util.concurrent.ListenableFuture;

import de.rennschnitzel.net.core.Connection;

public interface ConnectionFuture<C extends Connection> extends ListenableFuture<C> {

  boolean isOpen();

}
