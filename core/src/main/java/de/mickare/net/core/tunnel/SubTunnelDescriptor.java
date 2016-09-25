package de.mickare.net.core.tunnel;

import de.mickare.net.core.Tunnel;
import de.mickare.net.protocol.TransportProtocol;

public interface SubTunnelDescriptor<C extends SubTunnel> {
	
	String getName();
	
	TransportProtocol.TunnelRegister.Type getType();
	
	C create( Tunnel parentTunnel );
	
	C cast( SubTunnel tunnel );
	
}
