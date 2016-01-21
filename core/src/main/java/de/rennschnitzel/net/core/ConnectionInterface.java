package de.rennschnitzel.net.core;

import de.rennschnitzel.net.Owner;
import de.rennschnitzel.net.core.tunnel.SubTunnel;
import de.rennschnitzel.net.core.tunnel.SubChannelDescriptor;

public interface ConnectionInterface {

  AbstractNetwork getNetwork();

  Tunnel getChannel(String name);

  Tunnel getChannel(String name, boolean register);

  <S extends SubTunnel> S getChannelIfPresent(SubChannelDescriptor<S> descriptor);

  <S extends SubTunnel> S getChannel(SubChannelDescriptor<S> descriptor, Owner owner);

  boolean isClosed();

}
