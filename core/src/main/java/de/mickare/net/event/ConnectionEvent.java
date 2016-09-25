package de.mickare.net.event;

import com.google.common.base.Preconditions;

import de.mickare.net.core.Connection;
import lombok.Getter;

public @Getter class ConnectionEvent extends NetworkEvent {
	
	private final Connection connection;
	
	public ConnectionEvent( Connection connection ) {
		super( connection.getNetwork() );
		Preconditions.checkNotNull( connection );
		this.connection = connection;
	}
	
}
