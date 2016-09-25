package de.mickare.net.core.tunnel.custom;

import de.mickare.net.core.Tunnel;
import de.mickare.net.core.tunnel.AbstractSubTunnel;

public abstract class CustomSubTunnel<SELF extends CustomSubTunnel<SELF, D>, D extends CustomSubTunnelDescriptor<D, SELF>> extends AbstractSubTunnel<SELF, D> {
	
	public CustomSubTunnel( Tunnel parentTunnel, D descriptor ) {
		super( parentTunnel, descriptor );
	}
	
}
