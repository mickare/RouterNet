package de.rennschnitzel.backbone.api.network.message;

import de.rennschnitzel.backbone.net.protocol.TransportProtocol;

public interface PackableMessage {

  TransportProtocol.Packet toPacket();
  
}
