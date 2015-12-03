package de.rennschnitzel.backbone.api;

import com.google.common.util.concurrent.ListenableFuture;

public interface ConnectionFuture extends ListenableFuture<Connection> {

  boolean isSuccess();

  void cancelOrClose();
  
}
