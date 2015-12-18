package de.rennschnitzel.backbone.net.channel;

import de.rennschnitzel.backbone.Owner;
import de.rennschnitzel.backbone.net.NetworkMember;

public interface SubChannel {

  Owner getOwner();

  void close();

  boolean isClosed();

  int getChannelId();

  String getName();

  Channel getParentChannel();

  NetworkMember getHome();
  
  SubChannelDescriptor<?> getDescriptor();

}
