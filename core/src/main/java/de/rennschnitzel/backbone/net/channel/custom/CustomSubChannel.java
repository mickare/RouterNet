package de.rennschnitzel.backbone.net.channel.custom;

import de.rennschnitzel.backbone.net.channel.AbstractSubChannel;
import de.rennschnitzel.backbone.net.channel.Channel;

public abstract class CustomSubChannel<SELF extends CustomSubChannel<SELF, D>, D extends CustomSubChannelDescriptor<D, SELF>>
    extends AbstractSubChannel<SELF, D> {

  public CustomSubChannel(Channel parentChannel, D descriptor) {
    super(parentChannel, descriptor);
  }

}
