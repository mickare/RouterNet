package de.rennschnitzel.net.core.tunnel;

import de.rennschnitzel.net.core.Tunnel;
import de.rennschnitzel.net.protocol.TransportProtocol;

public interface TunnelHandler {
	
	Tunnel getParentTunnel();
	
	void receive( TunnelMessage msg ) throws Exception;
	
	TransportProtocol.TunnelRegister.Type getType();
	
}
