package de.rennschnitzel.backbone.net.channel;

import de.rennschnitzel.backbone.Owner;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;

public interface ChannelHandler<P, T> {

  void receive(T msg) throws Exception;

  Owner getOwner();
  
  TransportProtocol.ChannelRegister.Type getType();
  
}
