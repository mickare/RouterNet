package de.rennschnitzel.net.core.tunnel.custom;

import de.rennschnitzel.net.core.tunnel.AbstractSubTunnelDescriptor;
import de.rennschnitzel.net.protocol.TransportProtocol;

public abstract class CustomSubChannelDescriptor<SELF extends CustomSubChannelDescriptor<SELF, C>, C extends CustomSubChannel<C, SELF>>
    extends AbstractSubTunnelDescriptor<SELF, C> {

  public CustomSubChannelDescriptor(String name) {
    super(name, TransportProtocol.TunnelRegister.Type.CUSTOM);
  }

}
