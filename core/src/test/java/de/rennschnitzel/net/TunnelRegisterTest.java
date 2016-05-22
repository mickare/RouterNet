package de.rennschnitzel.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.client.ConnectClient;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.Target;
import de.rennschnitzel.net.core.login.AuthenticationFactory;
import de.rennschnitzel.net.core.login.ClientLoginEngine;
import de.rennschnitzel.net.core.login.RouterLoginEngine;
import de.rennschnitzel.net.core.packet.BasePacketHandler;
import de.rennschnitzel.net.dummy.DummClientNetwork;
import de.rennschnitzel.net.netty.ConnectionHandler;
import de.rennschnitzel.net.netty.LocalConnectClient;
import de.rennschnitzel.net.netty.LoginHandler;
import de.rennschnitzel.net.netty.PipelineUtils;
import de.rennschnitzel.net.util.FutureUtils;
import de.rennschnitzel.net.util.SimpleOwner;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Promise;

public class TunnelRegisterTest {
	
	Owner testingOwner;
	EventLoopGroup group = new DefaultEventLoopGroup();
	
	@Before
	public void setup() throws InterruptedException {
		
		testingOwner = new SimpleOwner( "TunnelTestOwner", Logger.getLogger( "TunnelTest" ) );
		
	}
	
	@After
	public void tearDown() {
		group.shutdownGracefully( 1, 100, TimeUnit.MILLISECONDS );
	}
	
	public class NetWrapper implements AutoCloseable {
		public final DummClientNetwork router;
		public final DummClientNetwork client;
		public final Target router_target;
		public final Target client_target;
		private final LocalConnectClient connection;
		private final Promise<Connection> connection_router = FutureUtils.newPromise();
		private final Promise<Connection> connection_client = FutureUtils.newPromise();
		
		public NetWrapper() {
			router = new DummClientNetwork( group, new UUID( 0, 1 ) );
			router.setName( "Router" );
			client = new DummClientNetwork( group, new UUID( 0, 2 ) );
			client.setName( "Client" );
			
			RouterLoginEngine routerEngine = new RouterLoginEngine( router, AuthenticationFactory.newPasswordForRouter( "pw" ) );
			ClientLoginEngine clientEngine = new ClientLoginEngine( client, AuthenticationFactory.newPasswordForClient( "pw" ) );
			
			router_target = Target.to( router.getHome().getId() );
			client_target = Target.to( client.getHome().getId() );
			
			connection = new LocalConnectClient( PipelineUtils.baseInitAnd( ch -> {
				ch.pipeline().addLast( new LoginHandler( routerEngine, connection_router ) );
				ch.pipeline().addLast( new ConnectionHandler( router, new BasePacketHandler() ) );
			} ), PipelineUtils.baseInitAnd( ch -> {
				ch.pipeline().addLast( new LoginHandler( clientEngine, connection_client ) );
				ch.pipeline().addLast( new ConnectionHandler( client, new BasePacketHandler() ) );
			} ), group );
		}
		
		public void connect() throws Exception {
			connection.connect();
			connection.awaitRunning();
			Preconditions.checkState( connection.getState() == ConnectClient.State.ACTIVE );
			
			client.awaitConnected( 1, TimeUnit.SECONDS );
			router.awaitConnected( 1, TimeUnit.SECONDS );
			
			Preconditions.checkNotNull( client.getNode( router.getHome().getId() ) );
			Preconditions.checkNotNull( router.getNode( client.getHome().getId() ) );
		}
		
		@Override
		public void close() throws Exception {
			connection.close();
		}
		
	}
	
	@Test
	public void testLateRegister() throws Exception {
		
		try ( NetWrapper test = new NetWrapper() ) {
			
			test.client.getTunnel( "base0" );
			test.router.getTunnel( "base1" );
			
			assertNull( test.client.getTunnelIfPresent( "base1" ) );
			assertNull( test.router.getTunnelIfPresent( "base0" ) );
			
			test.connect();
			
			assertTrue( test.client.awaitConnected( 1, TimeUnit.SECONDS ) );
			assertTrue( test.router.awaitConnected( 1, TimeUnit.SECONDS ) );
			
			test.connection_client.get( 1, TimeUnit.SECONDS ).awaitTunnelRegistered( "base1" );
			test.connection_router.get( 1, TimeUnit.SECONDS ).awaitTunnelRegistered( "base0" );
			
			assertEquals( test.client.getTunnelIfPresent( "base0" ).getName(), test.router.getTunnelIfPresent( "base0" ).getName() );
			assertEquals( test.router.getTunnelIfPresent( "base1" ).getName(), test.client.getTunnelIfPresent( "base1" ).getName() );
			
		}
		
	}
	
}
