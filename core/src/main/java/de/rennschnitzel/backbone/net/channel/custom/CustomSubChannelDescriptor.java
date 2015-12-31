package de.rennschnitzel.backbone.net.channel.custom;

import de.rennschnitzel.backbone.net.channel.AbstractSubChannelDescriptor;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;

public abstract class CustomSubChannelDescriptor<SELF extends CustomSubChannelDescriptor<SELF, C>, C extends CustomSubChannel<C, SELF>>
    extends AbstractSubChannelDescriptor<SELF, C> {

  public CustomSubChannelDescriptor(String name) {
    super(name, TransportProtocol.ChannelRegister.Type.CUSTOM);
  }

}
