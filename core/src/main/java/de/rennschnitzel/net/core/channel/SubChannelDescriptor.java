package de.rennschnitzel.net.core.channel;

import de.rennschnitzel.net.protocol.TransportProtocol;

public interface SubChannelDescriptor<C extends SubChannel> {

  String getName();

  TransportProtocol.ChannelRegister.Type getType();

  C create(Channel parentChannel);

  C cast(SubChannel channel);

}
