package de.rennschnitzel.net.core.tunnel;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.Tunnel;

public interface SubTunnel {

  void close();

  boolean isClosed();

  String getName();

  int getId();

  Tunnel getParentTunnel();

  SubTunnelDescriptor<?> getDescriptor();

  AbstractNetwork getNetwork();

  void register(Connection connection);

}
