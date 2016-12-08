package de.mickare.routernet.event;

import de.mickare.routernet.core.Connection;

public class ConnectionRemovedEvent extends ConnectionEvent {
	
	public ConnectionRemovedEvent( Connection connection ) {
		super( connection );
	}
}
