package de.mickare.routernet;

import java.util.logging.Logger;

import de.mickare.routernet.core.AbstractClientNetwork;
import lombok.Getter;

public class Network extends AbstractClientNetwork {
	
	private final @Getter NetClient client;
	
	public Network( NetClient client ) {
		super( client.getLogger(), client.getExecutor(), client.getHome() );
		this.client = client;
	}
	
	@Override
	public Logger getLogger() {
		return client.getLogger();
	}
	
	public void resetInstance() {
		super.setInstance( this );
	}
	
	@Override
	public void syncExecuteIfPossible( Runnable command ) {
		client.syncExecuteIfPossible( command );
	}
	
}
