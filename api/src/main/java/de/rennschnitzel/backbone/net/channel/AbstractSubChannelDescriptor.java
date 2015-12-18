package de.rennschnitzel.backbone.net.channel;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.Owner;
import de.rennschnitzel.backbone.net.channel.Channel;
import de.rennschnitzel.backbone.net.channel.SubChannel;
import de.rennschnitzel.backbone.net.channel.SubChannelDescriptor;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;
import lombok.Getter;

public abstract class AbstractSubChannelDescriptor<SELF extends AbstractSubChannelDescriptor<SELF, C>, C extends AbstractSubChannel<C, SELF>>
    implements SubChannelDescriptor<C> {

  @Getter
  protected final String name;
  @Getter
  protected final TransportProtocol.ChannelRegister.Type type;

  public AbstractSubChannelDescriptor(String name, TransportProtocol.ChannelRegister.Type type) {
    Preconditions.checkArgument(!name.isEmpty());
    Preconditions.checkNotNull(type);
    this.name = name.toLowerCase();
    this.type = type;
  }

  @Override
  public abstract C create(Owner owner, Channel parentChannel);

  @SuppressWarnings("unchecked")
  @Override
  public C cast(SubChannel channel) {
    Preconditions.checkArgument(channel.getDescriptor() == this);
    return (C) channel;
  }
  
  public abstract boolean equals(Object o);
  
  public abstract int hashCode();

}
