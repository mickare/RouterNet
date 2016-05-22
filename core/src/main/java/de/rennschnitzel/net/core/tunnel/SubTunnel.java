package de.rennschnitzel.net.core.tunnel;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.Tunnel;
import de.rennschnitzel.net.core.Connection;

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
