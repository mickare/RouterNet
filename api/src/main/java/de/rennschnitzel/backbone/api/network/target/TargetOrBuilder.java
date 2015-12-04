package de.rennschnitzel.backbone.api.network.target;

import de.rennschnitzel.backbone.api.network.Server;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;

public interface TargetOrBuilder extends MessageReceiver {

  TransportProtocol.Target toProtocol();

  boolean contains(Server home);

}
