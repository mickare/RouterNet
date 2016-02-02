package de.rennschnitzel.net.core.tunnel.custom;

import de.rennschnitzel.net.core.tunnel.AbstractSubTunnelDescriptor;
import de.rennschnitzel.net.protocol.TransportProtocol;

public abstract class CustomSubTunnelDescriptor<SELF extends CustomSubTunnelDescriptor<SELF, C>, C extends CustomSubTunnel<C, SELF>>
    extends AbstractSubTunnelDescriptor<SELF, C> {

  public CustomSubTunnelDescriptor(String name) {
    super(name, TransportProtocol.TunnelRegister.Type.CUSTOM);
  }

}
