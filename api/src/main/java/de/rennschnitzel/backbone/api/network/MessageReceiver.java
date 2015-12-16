package de.rennschnitzel.backbone.api.network;

public interface MessageReceiver {

  void send(String key, byte[] data);

  void send(Object object);

}
