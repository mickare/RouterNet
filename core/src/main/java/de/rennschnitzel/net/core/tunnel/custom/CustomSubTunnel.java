package de.rennschnitzel.net.core.tunnel.custom;

import de.rennschnitzel.net.core.Tunnel;
import de.rennschnitzel.net.core.tunnel.AbstractSubTunnel;

public abstract class CustomSubTunnel<SELF extends CustomSubTunnel<SELF, D>, D extends CustomSubTunnelDescriptor<D, SELF>>
    extends AbstractSubTunnel<SELF, D> {

  public CustomSubTunnel(Tunnel parentTunnel, D descriptor) {
    super(parentTunnel, descriptor);
  }

}
