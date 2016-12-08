package de.mickare.routernet.core.tunnel;

import de.mickare.routernet.core.Connection;
import de.mickare.routernet.core.Tunnel;
import de.mickare.routernet.protocol.TransportProtocol;

public interface TunnelHandler {
	
	Tunnel getParentTunnel();
	
	void receive( Connection con, TunnelMessage msg ) throws Exception;
	
	TransportProtocol.TunnelRegister.Type getType();
	
}
