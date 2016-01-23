package de.rennschnitzel.net.core.tunnel;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.Tunnel;
import io.netty.util.concurrent.Future;

public interface SubTunnel {

  void close();

  boolean isClosed();

  String getName();

  Tunnel getParentTunnel();

  SubTunnelDescriptor<?> getDescriptor();

  AbstractNetwork getNetwork();

  Future<Integer> register();
  
}
