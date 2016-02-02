package de.rennschnitzel.net.core.tunnel;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.Tunnel;
import de.rennschnitzel.net.protocol.TransportProtocol;
import lombok.Getter;

public abstract class AbstractSubTunnelDescriptor<SELF extends AbstractSubTunnelDescriptor<SELF, C>, C extends AbstractSubTunnel<C, SELF>>
    implements SubTunnelDescriptor<C> {

  protected @Getter final String name;
  protected @Getter final TransportProtocol.TunnelRegister.Type type;

  public AbstractSubTunnelDescriptor(String name, TransportProtocol.TunnelRegister.Type type) {
    Preconditions.checkArgument(!name.isEmpty());
    Preconditions.checkNotNull(type);
    this.name = name.toLowerCase();
    this.type = type;
  }

  @Override
  public abstract C create(Tunnel parentChannel);

  @SuppressWarnings("unchecked")
  @Override
  public C cast(SubTunnel channel) {
    if (channel == null) {
      return null;
    }
    Preconditions.checkArgument(channel.getDescriptor() == this);
    return (C) channel;
  }

  public abstract boolean equals(Object o);

  public abstract int hashCode();

}
