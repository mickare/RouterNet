package de.mickare.net.core.tunnel;

import de.mickare.net.core.AbstractNetwork;
import de.mickare.net.core.Connection;
import de.mickare.net.core.Tunnel;

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
