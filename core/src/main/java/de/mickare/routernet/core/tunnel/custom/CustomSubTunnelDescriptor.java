package de.mickare.routernet.core.tunnel.custom;

import de.mickare.routernet.core.tunnel.AbstractSubTunnelDescriptor;
import de.mickare.routernet.protocol.TransportProtocol;

public abstract class CustomSubTunnelDescriptor<SELF extends CustomSubTunnelDescriptor<SELF, C>, C extends CustomSubTunnel<C, SELF>> extends AbstractSubTunnelDescriptor<SELF, C> {
	
	public CustomSubTunnelDescriptor( String name ) {
		super( name, TransportProtocol.TunnelRegister.Type.CUSTOM );
	}
	
}
