package de.rennschnitzel.net.core;

import de.rennschnitzel.net.Owner;
import de.rennschnitzel.net.core.channel.Channel;
import de.rennschnitzel.net.core.channel.SubChannel;
import de.rennschnitzel.net.core.channel.SubChannelDescriptor;

public interface ConnectionInterface {

  AbstractNetwork getNetwork();

  Channel getChannel(String name);

  Channel getChannel(String name, boolean register);

  <S extends SubChannel> S getChannelIfPresent(SubChannelDescriptor<S> descriptor);

  <S extends SubChannel> S getChannel(SubChannelDescriptor<S> descriptor, Owner owner);

  boolean isClosed();

}
