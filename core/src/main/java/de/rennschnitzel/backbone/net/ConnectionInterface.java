package de.rennschnitzel.backbone.net;

import de.rennschnitzel.backbone.Owner;
import de.rennschnitzel.backbone.net.channel.Channel;
import de.rennschnitzel.backbone.net.channel.SubChannel;
import de.rennschnitzel.backbone.net.channel.SubChannelDescriptor;

public interface ConnectionInterface {

  AbstractNetwork getNetwork();

  Channel getChannel(String name);

  Channel getChannel(String name, boolean register);

  <S extends SubChannel> S getChannelIfPresent(SubChannelDescriptor<S> descriptor);

  <S extends SubChannel> S getChannel(SubChannelDescriptor<S> descriptor, Owner owner);

  boolean isClosed();

}
