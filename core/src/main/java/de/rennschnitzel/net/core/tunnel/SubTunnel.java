package de.rennschnitzel.net.core.tunnel;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.Tunnel;

public interface SubTunnel {

  void close();

  boolean isClosed();

  String getName();

  Tunnel getParentTunnel();

  SubChannelDescriptor<?> getDescriptor();

  AbstractNetwork getNetwork();

}
