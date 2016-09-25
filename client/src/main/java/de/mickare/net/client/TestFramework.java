package de.mickare.net.client;

import java.util.UUID;

import com.google.common.base.Preconditions;

import de.mickare.net.NetClient;
import de.mickare.net.core.Node.HomeNode;
import de.mickare.net.core.login.AuthenticationFactory;
import de.mickare.net.core.login.RouterAuthentication;
import de.mickare.net.dummy.DummClientNetwork;
import lombok.Getter;

public class TestFramework {
	
	@Getter
	private final NetClient testClient;
	
	@Getter
	private final DummClientNetwork testNetwork;
	
	@Getter
	private RouterAuthentication authenticationRouter;
	
	public TestFramework( NetClient client ) {
		Preconditions.checkNotNull( client );
		this.testClient = client;
		this.authenticationRouter = AuthenticationFactory.newPasswordForRouter( client.getConfig().getConnection().getPassword() );
		
		UUID temp;
		do {
			temp = UUID.randomUUID();
		} while ( client.getHome().getId().equals( temp ) );
		
		this.testNetwork = new DummClientNetwork( client.getExecutor(), new HomeNode( temp ) );
		this.testNetwork.setName( "TestNetwork" );
		
	}
	
}
