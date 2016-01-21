package de.rennschnitzel.net.core.tunnel;

import de.rennschnitzel.net.core.Tunnel;
import de.rennschnitzel.net.protocol.TransportProtocol;

public interface SubChannelDescriptor<C extends SubTunnel> {

  String getName();

  TransportProtocol.TunnelRegister.Type getType();

  C create(Tunnel parentChannel);

  C cast(SubTunnel channel);

}
