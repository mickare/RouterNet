package de.rennschnitzel.backbone.net.channel;

import de.rennschnitzel.backbone.Owner;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;

public interface ChannelHandler {

  Channel getParentChannel();

  void receive(ChannelMessage msg) throws Exception;

  Owner getOwner();

  TransportProtocol.ChannelRegister.Type getType();

}
