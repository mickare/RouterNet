package de.mickare.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.mickare.routernet.Owner;
import de.mickare.routernet.core.Connection;
import de.mickare.routernet.core.login.AuthenticationFactory;
import de.mickare.routernet.core.login.ClientLoginEngine;
import de.mickare.routernet.core.login.LoginEngine;
import de.mickare.routernet.core.login.RouterLoginEngine;
import de.mickare.routernet.core.login.LoginEngine.State;
import de.mickare.routernet.dummy.DummClientNetwork;
import de.mickare.routernet.exception.HandshakeException;
import de.mickare.routernet.netty.LocalConnectClient;
import de.mickare.routernet.netty.LoginHandler;
import de.mickare.routernet.netty.PipelineUtils;
import de.mickare.routernet.util.FutureUtils;
import de.mickare.routernet.util.SimpleOwner;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Promise;

public class LoginTest {
	
	Owner testingOwner;
	DummClientNetwork net_router;
	DummClientNetwork net_client;
	EventLoopGroup group = new DefaultEventLoopGroup();
	
	@Before
	public void setup() {
		
		testingOwner = new SimpleOwner( "LoginTestOwner", Logger.getLogger( "LoginTest" ) );
		
		net_router = new DummClientNetwork( group );
		net_router.setName( "Router" );
		net_client = new DummClientNetwork( group, net_router.newNotUsedUUID() );
		net_client.setName( "Client" );
		
	}
	
	@After
	public void tearDown() {
		group.shutdownGracefully();
	}
	
	@Test
	public void testLoginSuccessful() throws Throwable {
		
		final String password = "testLogin";
		
		RouterLoginEngine routerEngine = new RouterLoginEngine( net_router, AuthenticationFactory.newPasswordForRouter( password ) );
		ClientLoginEngine clientEngine = new ClientLoginEngine( net_client, AuthenticationFactory.newPasswordForClient( password ) );
		
		final Promise<Connection> con_router = FutureUtils.newPromise();
		final Promise<Connection> con_client = FutureUtils.newPromise();
		
		LocalConnectClient con = new LocalConnectClient( PipelineUtils.baseInitAnd( ch -> {
			ch.pipeline().addLast( new LoginHandler( routerEngine, con_router ) );
		} ), PipelineUtils.baseInitAnd( ch -> {
			ch.pipeline().addLast( new LoginHandler( clientEngine, con_client ) );
		} ), group );
		
		try ( AutoCloseable l = con.connect() ) {
			con.awaitRunning();
			
			con_client.await( 1000 );
			con_router.await( 1000 );
			
			if ( routerEngine.getFailureCause() != null ) {
				throw routerEngine.getFailureCause();
			}
			if ( clientEngine.getFailureCause() != null ) {
				throw clientEngine.getFailureCause();
			}
			
			System.out.println( routerEngine.getState() );
			
			assertTrue( routerEngine.isDone() );
			assertTrue( clientEngine.isDone() );
			assertEquals( State.SUCCESS, routerEngine.getState() );
			assertEquals( State.SUCCESS, clientEngine.getState() );
			assertEquals( net_client.getHome().getId(), routerEngine.getLoginId() );
			assertEquals( net_router.getHome().getId(), clientEngine.getLoginId() );
			
		} finally {
			con.close();
		}
		
	}
	
	@Test
	public void testLoginFailed() throws Exception {
		
		final String password1 = "testLogin";
		final String password2 = "falsePassword";
		
		RouterLoginEngine routerEngine = new RouterLoginEngine( net_router, AuthenticationFactory.newPasswordForRouter( password1 ) );
		ClientLoginEngine clientEngine = new ClientLoginEngine( net_client, AuthenticationFactory.newPasswordForClient( password2 ) );
		
		final Promise<Connection> con_router = FutureUtils.newPromise();
		final Promise<Connection> con_client = FutureUtils.newPromise();
		
		LocalConnectClient con = new LocalConnectClient( PipelineUtils.baseInitAnd( ch -> {
			ch.pipeline().addLast( new LoginHandler( routerEngine, con_router ) );
		} ), PipelineUtils.baseInitAnd( ch -> {
			ch.pipeline().addLast( new LoginHandler( clientEngine, con_client ) );
		} ) );
		
		try ( AutoCloseable l = con.connect() ) {
			con.awaitRunning();
			
			con_router.await( 1000 );
			
			assertTrue( routerEngine.isDone() );
			assertFalse( routerEngine.isSuccess() );
			assertTrue( con_router.isDone() );
			assertFalse( con_router.isSuccess() );
			
			con_client.await( 1000 );
			
			assertTrue( clientEngine.isDone() );
			assertFalse( clientEngine.isSuccess() );
			assertTrue( con_client.isDone() );
			assertFalse( con_client.isSuccess() );
			
			assertTrue( routerEngine.isDone() );
			assertTrue( clientEngine.isDone() );
			assertEquals( State.FAILED, routerEngine.getState() );
			assertEquals( State.FAILED, clientEngine.getState() );
			
			assertEquals( LoginEngine.State.AUTH, routerEngine.getFailureState() );
			assertEquals( LoginEngine.State.AUTH, clientEngine.getFailureState() );
			assertEquals( HandshakeException.class, routerEngine.getFailureCause().getClass() );
			assertEquals( HandshakeException.class, clientEngine.getFailureCause().getClass() );
			
			assertEquals( "invalid login", routerEngine.getFailureCause().getMessage() );
			assertEquals( "invalid login", clientEngine.getFailureCause().getMessage() );
			
		} finally {
			con.close();
		}
		
	}
	
}
