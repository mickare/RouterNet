package de.mickare.routernet.core.tunnel.custom;

import de.mickare.routernet.core.Tunnel;
import de.mickare.routernet.core.tunnel.AbstractSubTunnel;

public abstract class CustomSubTunnel<SELF extends CustomSubTunnel<SELF, D>, D extends CustomSubTunnelDescriptor<D, SELF>> extends AbstractSubTunnel<SELF, D> {
	
	public CustomSubTunnel( Tunnel parentTunnel, D descriptor ) {
		super( parentTunnel, descriptor );
	}
	
}
