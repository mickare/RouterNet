package de.mickare.routernet.core.tunnel;

import de.mickare.routernet.core.Tunnel;
import de.mickare.routernet.protocol.TransportProtocol;

public interface SubTunnelDescriptor<C extends SubTunnel> {
	
	String getName();
	
	TransportProtocol.TunnelRegister.Type getType();
	
	C create( Tunnel parentTunnel );
	
	C cast( SubTunnel tunnel );
	
}
