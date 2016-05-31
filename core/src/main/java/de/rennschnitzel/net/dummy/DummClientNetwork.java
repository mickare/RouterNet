package de.rennschnitzel.net.dummy;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

import de.rennschnitzel.net.core.AbstractClientNetwork;
import de.rennschnitzel.net.core.Node.HomeNode;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeMessage.Type;

public class DummClientNetwork extends AbstractClientNetwork {
	
	private static Logger LOGGER_DEFAULT = new DummyLogger( "DummyNetwork", System.out );
	
	public DummClientNetwork( ScheduledExecutorService executor ) {
		this( executor, new HomeNode( UUID.randomUUID() ) );
	}
	
	public DummClientNetwork( ScheduledExecutorService executor, UUID uuid ) {
		this( executor, new HomeNode( uuid ) );
	}
	
	public DummClientNetwork( ScheduledExecutorService executor, HomeNode home ) {
		super( LOGGER_DEFAULT, executor, home );
		home.setType( Type.BUKKIT );
	}
	
	public void setName( String name ) {
		this.setLogger( new DummyLogger( name, System.out ) );
	}
	
	public UUID newNotUsedUUID() {
		UUID result;
		do {
			result = UUID.randomUUID();
		} while ( this.getNode( result ) != null );
		return result;
	}
	
}
