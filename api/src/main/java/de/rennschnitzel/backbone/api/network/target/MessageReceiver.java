package de.rennschnitzel.backbone.api.network.target;

import java.util.Map;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.MessageOrBuilder;

import de.rennschnitzel.backbone.api.network.Server;

public interface MessageReceiver {

  void send(String key, byte[] data);

  void send(Object object);

  void send(MessageOrBuilder message);

  <T, R> Map<Server, ListenableFuture<T>> callServices(String name, Class<T> argument, Class<R> result);

}
