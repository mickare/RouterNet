package de.rennschnitzel.net.core.channel;

import de.rennschnitzel.net.core.AbstractNetwork;

public interface SubChannel {

  void close();

  boolean isClosed();

  int getChannelId();

  String getName();

  Channel getParentChannel();

  SubChannelDescriptor<?> getDescriptor();

  AbstractNetwork getNetwork();

}
