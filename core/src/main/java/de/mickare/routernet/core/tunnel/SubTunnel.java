package de.mickare.routernet.core.tunnel;

import de.mickare.routernet.core.AbstractNetwork;
import de.mickare.routernet.core.Connection;
import de.mickare.routernet.core.Tunnel;

public interface SubTunnel {
	
	void close();
	
	boolean isClosed();
	
	String getName();
	
	int getId();
	
	Tunnel getParentTunnel();
	
	SubTunnelDescriptor<?> getDescriptor();
	
	AbstractNetwork getNetwork();
	
	default void sendRegister( Connection connection ) {
		sendRegister( connection, true );
	}
	
	void sendRegister( Connection connection, boolean flush );
	
}
