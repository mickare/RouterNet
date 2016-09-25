package de.mickare.net.core.tunnel;

import de.mickare.net.core.Connection;
import de.mickare.net.core.Tunnel;
import de.mickare.net.protocol.TransportProtocol;

public interface TunnelHandler {
	
	Tunnel getParentTunnel();
	
	void receive( Connection con, TunnelMessage msg ) throws Exception;
	
	TransportProtocol.TunnelRegister.Type getType();
	
}
