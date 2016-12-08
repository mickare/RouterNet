package de.mickare.routernet.event;

import de.mickare.routernet.core.Connection;

public class ConnectionAddedEvent extends ConnectionEvent {
	
	public ConnectionAddedEvent( Connection connection ) {
		super( connection );
	}
}
