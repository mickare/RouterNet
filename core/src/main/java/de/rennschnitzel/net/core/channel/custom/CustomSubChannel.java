package de.rennschnitzel.net.core.channel.custom;

import de.rennschnitzel.net.core.Tunnel;
import de.rennschnitzel.net.core.tunnel.AbstractSubTunnel;

public abstract class CustomSubChannel<SELF extends CustomSubChannel<SELF, D>, D extends CustomSubChannelDescriptor<D, SELF>>
    extends AbstractSubTunnel<SELF, D> {

  public CustomSubChannel(Tunnel parentChannel, D descriptor) {
    super(parentChannel, descriptor);
  }

}
