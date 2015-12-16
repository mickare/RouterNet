package de.rennschnitzel.backbone.api.network.target;

import de.rennschnitzel.backbone.api.network.MessageReceiver;
import de.rennschnitzel.backbone.api.network.Server;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;

public interface TargetOrBuilder extends MessageReceiver {

  TransportProtocol.TargetMessage toProtocol();

  boolean contains(Server home);
  
  Target build();

}
