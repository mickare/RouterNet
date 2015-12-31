package de.rennschnitzel.backbone.net.channel;

import de.rennschnitzel.backbone.Owner;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;

public interface SubChannelDescriptor<C extends SubChannel> {

  String getName();

  TransportProtocol.ChannelRegister.Type getType();

  C create(Owner owner, Channel parentChannel);

  C cast(SubChannel channel);

}
