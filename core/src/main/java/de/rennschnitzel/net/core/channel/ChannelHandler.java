package de.rennschnitzel.net.core.channel;

import de.rennschnitzel.net.protocol.TransportProtocol;

public interface ChannelHandler {

  Channel getParentChannel();

  void receive(ChannelMessage msg) throws Exception;

  TransportProtocol.ChannelRegister.Type getType();

}
