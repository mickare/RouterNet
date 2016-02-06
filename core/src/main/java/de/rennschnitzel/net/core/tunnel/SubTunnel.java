package de.rennschnitzel.net.core.tunnel;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.Tunnel;
import de.rennschnitzel.net.core.Connection;

public interface SubTunnel {

  void close();

  boolean isClosed();

  String getName();

  int getId();

  Tunnel getParentTunnel();

  SubTunnelDescriptor<?> getDescriptor();

  AbstractNetwork getNetwork();

  default void register(Connection connection) {
    register(connection, true);
  }

  void register(Connection connection, boolean flush);

}
