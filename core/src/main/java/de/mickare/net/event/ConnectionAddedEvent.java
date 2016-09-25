package de.mickare.net.event;

import de.mickare.net.core.Connection;

public class ConnectionAddedEvent extends ConnectionEvent {
	
	public ConnectionAddedEvent( Connection connection ) {
		super( connection );
	}
}
