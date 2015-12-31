package de.rennschnitzel.backbone.net.channel.custom;

import de.rennschnitzel.backbone.Owner;
import de.rennschnitzel.backbone.net.channel.AbstractSubChannel;
import de.rennschnitzel.backbone.net.channel.Channel;

public abstract class CustomSubChannel<SELF extends CustomSubChannel<SELF, D>, D extends CustomSubChannelDescriptor<D, SELF>>
    extends AbstractSubChannel<SELF, D> {

  public CustomSubChannel(Owner owner, Channel parentChannel, D descriptor) {
    super(owner, parentChannel, descriptor);
  }

}
