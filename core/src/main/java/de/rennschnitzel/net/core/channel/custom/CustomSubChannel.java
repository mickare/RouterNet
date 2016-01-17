package de.rennschnitzel.net.core.channel.custom;

import de.rennschnitzel.net.core.channel.AbstractSubChannel;
import de.rennschnitzel.net.core.channel.Channel;

public abstract class CustomSubChannel<SELF extends CustomSubChannel<SELF, D>, D extends CustomSubChannelDescriptor<D, SELF>>
    extends AbstractSubChannel<SELF, D> {

  public CustomSubChannel(Channel parentChannel, D descriptor) {
    super(parentChannel, descriptor);
  }

}
