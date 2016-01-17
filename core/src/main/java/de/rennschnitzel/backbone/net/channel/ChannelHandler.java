package de.rennschnitzel.backbone.net.channel;

import de.rennschnitzel.backbone.net.protocol.TransportProtocol;

public interface ChannelHandler {

  Channel getParentChannel();

  void receive(ChannelMessage msg) throws Exception;

  TransportProtocol.ChannelRegister.Type getType();

}
