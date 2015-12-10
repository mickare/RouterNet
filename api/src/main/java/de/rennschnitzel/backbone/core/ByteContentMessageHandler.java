package de.rennschnitzel.backbone.core;

import de.rennschnitzel.backbone.ProtocolUtils;
import de.rennschnitzel.backbone.api.network.target.Target;
import de.rennschnitzel.backbone.net.protocol.ComponentUUID;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;

public class ByteContentMessageHandler {

  public void handle(TransportProtocol.TargetMessage target, ComponentUUID.UUID sender, TransportProtocol.ByteContent bytes) {
    receive(new Target(target), ProtocolUtils.convert(sender), bytes.getKey(), bytes.getData().toByteArray());
  }


}
