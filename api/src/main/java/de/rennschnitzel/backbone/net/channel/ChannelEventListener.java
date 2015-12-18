package de.rennschnitzel.backbone.net.channel;

public interface ChannelEventListener {
  
  void onConnect(Channel channel);

  void onDisconnect(Channel channel);

}