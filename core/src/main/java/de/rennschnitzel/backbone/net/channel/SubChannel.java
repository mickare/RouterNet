package de.rennschnitzel.backbone.net.channel;

import de.rennschnitzel.backbone.net.AbstractNetwork;

public interface SubChannel {

  void close();

  boolean isClosed();

  int getChannelId();

  String getName();

  Channel getParentChannel();

  SubChannelDescriptor<?> getDescriptor();

  AbstractNetwork getNetwork();

}
