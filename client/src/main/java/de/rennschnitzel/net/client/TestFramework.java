package de.rennschnitzel.net.client;

import java.util.UUID;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.NetClient;
import de.rennschnitzel.net.core.Node.HomeNode;
import de.rennschnitzel.net.core.login.AuthenticationFactory;
import de.rennschnitzel.net.core.login.RouterAuthentication;
import de.rennschnitzel.net.dummy.DummClientNetwork;
import lombok.Getter;

public class TestFramework {
	
	@Getter
	private final NetClient client;
	
	@Getter
	private final DummClientNetwork routerNetwork;
	
	@Getter
	private RouterAuthentication authenticationRouter;
	
	public TestFramework( NetClient client ) {
		Preconditions.checkNotNull( client );
		this.client = client;
		this.authenticationRouter = AuthenticationFactory.newPasswordForRouter( client.getConfig().getConnection().getPassword() );
		
		UUID temp;
		do {
			temp = UUID.randomUUID();
		} while ( client.getHome().getId().equals( temp ) );
		
		this.routerNetwork = new DummClientNetwork( client.getExecutor(), new HomeNode( temp ) );
		this.routerNetwork.setName( "TestNetwork" );
		
	}
	
}
