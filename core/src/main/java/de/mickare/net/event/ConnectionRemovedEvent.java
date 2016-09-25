package de.mickare.net.event;

import de.mickare.net.core.Connection;

public class ConnectionRemovedEvent extends ConnectionEvent {
	
	public ConnectionRemovedEvent( Connection connection ) {
		super( connection );
	}
}
