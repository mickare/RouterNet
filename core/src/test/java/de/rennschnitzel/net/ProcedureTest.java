package de.rennschnitzel.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.client.ConnectClient;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.Node;
import de.rennschnitzel.net.core.Target;
import de.rennschnitzel.net.core.login.AuthenticationFactory;
import de.rennschnitzel.net.core.login.ClientLoginEngine;
import de.rennschnitzel.net.core.login.RouterLoginEngine;
import de.rennschnitzel.net.core.packet.BasePacketHandler;
import de.rennschnitzel.net.core.procedure.BoundProcedure;
import de.rennschnitzel.net.core.procedure.CallableProcedure;
import de.rennschnitzel.net.core.procedure.CallableRegisteredProcedure;
import de.rennschnitzel.net.core.procedure.MultiProcedureCall;
import de.rennschnitzel.net.core.procedure.Procedure;
import de.rennschnitzel.net.core.procedure.ProcedureCallResult;
import de.rennschnitzel.net.core.procedure.SingleProcedureCall;
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

public class ProcedureTest {
	
	Owner testingOwner;
	
	DummClientNetwork net_router;
	DummClientNetwork net_client;
	
	Target target_client;
	Target target_router;
	
	private LocalConnectClient con;
	EventLoopGroup group = new DefaultEventLoopGroup();
	
	@Before
	public void setup() throws Throwable {
				
		testingOwner = new SimpleOwner( "ProcedureTestOwner", Logger.getLogger( "ProcedureTest" ) );
		
		net_router = new DummClientNetwork( group, new UUID( 0, 1 ) );
		net_router.setName( "Router" );
		net_client = new DummClientNetwork( group, new UUID( 0, 2 ) );
		net_client.setName( "Client" );
		
		RouterLoginEngine routerEngine = new RouterLoginEngine( net_router, AuthenticationFactory.newPasswordForRouter( "pw" ) );
		ClientLoginEngine clientEngine = new ClientLoginEngine( net_client, AuthenticationFactory.newPasswordForClient( "pw" ) );
		
		target_client = Target.to( net_client.getHome().getId() );
		target_router = Target.to( net_router.getHome().getId() );
		
		final Promise<Connection> con_router = FutureUtils.newPromise();
		final Promise<Connection> con_client = FutureUtils.newPromise();
		
		con = new LocalConnectClient( PipelineUtils.baseInitAnd( ch -> {
			ch.pipeline().addLast( new LoginHandler( routerEngine, con_router ) );
			ch.pipeline().addLast( new ConnectionHandler( net_router, new BasePacketHandler() ) );
		} ), PipelineUtils.baseInitAnd( ch -> {
			ch.pipeline().addLast( new LoginHandler( clientEngine, con_client ) );
			ch.pipeline().addLast( new ConnectionHandler( net_client, new BasePacketHandler() ) );
		} ), group );
		
		con.connect();
		con.awaitRunning();
		if ( con.getState() == ConnectClient.State.FAILED ) {
			throw con.getFailureCause();
		}
		Preconditions.checkState( con.getState() == ConnectClient.State.ACTIVE );
		
		net_client.awaitConnected( 1, TimeUnit.SECONDS );
		net_router.awaitConnected( 1, TimeUnit.SECONDS );
		
		Preconditions.checkNotNull( net_client.getNode( net_router.getHome().getId() ) );
		Preconditions.checkNotNull( net_router.getNode( net_client.getHome().getId() ) );
		
	}
	
	@After
	public void tearDown() {
		con.close();
		group.shutdownGracefully( 1, 100, TimeUnit.MILLISECONDS );
	}
	
	@Test
	public void testUsability() throws InterruptedException, TimeoutException, ExecutionException {
		
		Node node = net_client.getNode( net_router.getHome().getId() );
		
		Function<String, String> usability = ( str ) -> str + "success";
		BoundProcedure<String, String> proc = Procedure.of( "usability", usability );
		net_router.getProcedureManager().register( proc ).getRegisterFuture().await( 1000 );
		assertTrue( node.awaitProcedure( proc, 1, TimeUnit.SECONDS ));
		
		assertNotNull( node );
		assertTrue( node.hasProcedure( proc ) );
		
		String result = Procedure.of( "usability", usability )//
				.call( net_client.getNode( net_router.getHome().getId() ), "test" ).getResult().get( 1, TimeUnit.SECONDS );
		assertEquals( "testsuccess", result );
		
	}
	
	@Test
	public void testTypetools() throws IOException, InterruptedException, TimeoutException, ExecutionException {
		
		Node client_on_router = net_router.getNode( net_client.getHome().getId() );
		
		String helloWorld = "Hello World!";
		final AtomicInteger runCount = new AtomicInteger( 0 );
		
		Function<String, Integer> func = ( s ) -> {
			int i = runCount.incrementAndGet();
			return i;
		};
		net_client.getProcedureManager().register( "testTypetools", func ).getRegisterFuture().await( 1000 );
		
		Procedure info1 = Procedure.of( "testTypetools", String.class, Integer.class );
		Procedure info2 = Procedure.of( "testTypetools", func );
		
		assertEquals( info1, info2 );
		assertTrue( info1.equals( info2 ) );
		assertTrue( info1.compareTo( info2 ) == 0 );
		CallableRegisteredProcedure<?, ?> p = net_client.getProcedureManager().getRegistered( info1 );
		
		assertNotNull( p );
		assertTrue( p == net_client.getProcedureManager().getRegistered( info2 ) );
		assertTrue( net_client.getHome().hasProcedure( info1 ) );
		assertTrue( net_client.getHome().hasProcedure( info2 ) );
		
		assertNotNull( client_on_router );
		assertTrue( client_on_router.awaitProcedure( info1, 1, TimeUnit.SECONDS ));
		assertTrue( client_on_router.getProcedures().size() > 0 );
		assertTrue( client_on_router.hasProcedure( info1 ) );
		assertTrue( client_on_router.hasProcedure( info2 ) );
		
		CallableProcedure<String, Integer> p1 = info1.bind( net_router, func );
		CallableProcedure<String, Integer> p2 = info1.bind( net_router, String.class, Integer.class );
		assertEquals( p1, p2 );
		
		assertEquals( 0, runCount.get() );
		SingleProcedureCall<String, Integer> call = p1.call( client_on_router, helloWorld );
		assertEquals( 1, call.getResult().get( 1, TimeUnit.MILLISECONDS ).intValue() );
		assertEquals( 1, runCount.get() );
		
	}
	
	@Test
	public void testFunction() throws IOException, InterruptedException, TimeoutException, ExecutionException {
		
		Node client_on_router = net_router.getNode( net_client.getHome().getId() );
		
		String helloWorld = "Hello World!";
		final AtomicInteger runCount = new AtomicInteger( 0 );
		Function<String, String> func = ( t ) -> {
			runCount.incrementAndGet();
			return t;
		};
		
		net_client.getProcedureManager().register( "testFunction", func ).getRegisterFuture().await( 1000 );
		
		Procedure info1 = Procedure.of( "testFunction", String.class, String.class );
		Procedure info2 = Procedure.of( "testFunction", func );
		
		assertEquals( info1, info2 );
		assertTrue( info1.equals( info2 ) );
		assertTrue( info1.compareTo( info2 ) == 0 );
		CallableRegisteredProcedure<?, ?> p = net_client.getProcedureManager().getRegistered( info1 );
		
		assertNotNull( p );
		assertTrue( p == net_client.getProcedureManager().getRegistered( info2 ) );
		assertTrue( net_client.getHome().hasProcedure( info1 ) );
		assertTrue( net_client.getHome().hasProcedure( info2 ) );
		
		assertNotNull( client_on_router );
		assertTrue( client_on_router.awaitProcedure( info1, 1, TimeUnit.SECONDS ));
		assertTrue( client_on_router.getProcedures().size() > 0 );
		assertTrue( client_on_router.hasProcedure( info1 ) );
		assertTrue( client_on_router.hasProcedure( info2 ) );
		
		CallableProcedure<String, String> p1 = info1.bind( net_router, func );
		CallableProcedure<String, String> p2 = info1.bind( net_router, String.class, String.class );
		assertEquals( p1, p2 );
		
		assertEquals( 0, runCount.get() );
		SingleProcedureCall<String, String> call = p1.call( client_on_router, helloWorld );
		assertEquals( helloWorld, call.getResult().get( 1, TimeUnit.MILLISECONDS ) );
		assertEquals( 1, runCount.get() );
		
	}
	
	@Test
	public void testRunnable() throws InterruptedException, TimeoutException, ExecutionException {
		
		Node client_on_router = net_router.getNode( net_client.getHome().getId() );
		
		final AtomicInteger runCount = new AtomicInteger( 0 );
		Runnable func = () -> {
			runCount.incrementAndGet();
		};
		net_client.getProcedureManager().register( "testRunnable", func ).getRegisterFuture().await( 1000 );
		
		Procedure info1 = Procedure.of( "testRunnable", Void.class, Void.class );
		Procedure info2 = Procedure.of( "testRunnable", func );
		
		assertEquals( info1, info2 );
		assertTrue( info1.equals( info2 ) );
		assertTrue( info1.compareTo( info2 ) == 0 );
		CallableRegisteredProcedure<?, ?> p = net_client.getProcedureManager().getRegistered( info1 );
		
		assertNotNull( p );
		assertTrue( p == net_client.getProcedureManager().getRegistered( info2 ) );
		assertTrue( net_client.getHome().hasProcedure( info1 ) );
		assertTrue( net_client.getHome().hasProcedure( info2 ) );
		
		assertNotNull( client_on_router );
		assertTrue( client_on_router.awaitProcedure( info1, 1, TimeUnit.SECONDS ));
		assertTrue( client_on_router.getProcedures().size() > 0 );
		assertTrue( client_on_router.hasProcedure( info1 ) );
		assertTrue( client_on_router.hasProcedure( info2 ) );
		
		CallableProcedure<Void, Void> p1 = info1.bind( net_router, func );
		CallableProcedure<Void, Void> p2 = info1.bind( net_router, Void.class, Void.class );
		assertEquals( p1, p2 );
		
		assertEquals( 0, runCount.get() );
		SingleProcedureCall<Void, Void> call = p1.call( client_on_router, null );
		assertEquals( null, call.getResult().get( 1, TimeUnit.MILLISECONDS ) );
		assertEquals( 1, runCount.get() );
		
	}
	
	@Test
	public void testConsumer() throws InterruptedException, TimeoutException, ExecutionException {
		
		Node client_on_router = net_router.getNode( net_client.getHome().getId() );
		
		String helloWorld = "Hello World!";
		final AtomicInteger runCount = new AtomicInteger( 0 );
		Consumer<String> func = ( t ) -> {
			if ( helloWorld.equals( t ) ) {
				runCount.incrementAndGet();
			} else {
				fail();
			}
		};
		net_client.getProcedureManager().register( "testConsumer", func ).getRegisterFuture().await( 1000 );
		
		Procedure info1 = Procedure.of( "testConsumer", String.class, Void.class );
		Procedure info2 = Procedure.of( "testConsumer", func );
		
		assertEquals( info1, info2 );
		assertTrue( info1.equals( info2 ) );
		assertTrue( info1.compareTo( info2 ) == 0 );
		CallableRegisteredProcedure<?, ?> p = net_client.getProcedureManager().getRegistered( info1 );
		
		assertNotNull( p );
		assertTrue( p == net_client.getProcedureManager().getRegistered( info2 ) );
		assertTrue( net_client.getHome().hasProcedure( info1 ) );
		assertTrue( net_client.getHome().hasProcedure( info2 ) );
		
		assertNotNull( client_on_router );
		assertTrue( client_on_router.awaitProcedure( info1, 1, TimeUnit.SECONDS ));
		assertTrue( client_on_router.getProcedures().size() > 0 );
		assertTrue( client_on_router.hasProcedure( info1 ) );
		assertTrue( client_on_router.hasProcedure( info2 ) );
		
		CallableProcedure<String, Void> p1 = info1.bind( net_router, func );
		CallableProcedure<String, Void> p2 = info1.bind( net_router, String.class, Void.class );
		assertEquals( p1, p2 );
		
		assertEquals( 0, runCount.get() );
		SingleProcedureCall<String, Void> call = p1.call( client_on_router, helloWorld );
		assertEquals( null, call.getResult().get( 1, TimeUnit.MILLISECONDS ) );
		assertEquals( 1, runCount.get() );
		
	}
	
	@Test
	public void testSupplier() throws InterruptedException, TimeoutException, ExecutionException {
		
		Node client_on_router = net_router.getNode( net_client.getHome().getId() );
		
		String helloWorld = "Hello World!";
		final AtomicInteger runCount = new AtomicInteger( 0 );
		Supplier<String> func = () -> {
			runCount.incrementAndGet();
			return helloWorld;
		};
		
		net_client.getProcedureManager().register( "testSupplier", func ).getRegisterFuture().await( 1000 );
		
		Procedure info1 = Procedure.of( "testSupplier", Void.class, String.class );
		Procedure info2 = Procedure.of( "testSupplier", func );
		
		assertEquals( info1, info2 );
		assertTrue( info1.equals( info2 ) );
		assertTrue( info1.compareTo( info2 ) == 0 );
		CallableRegisteredProcedure<?, ?> p = net_client.getProcedureManager().getRegistered( info1 );
		
		assertNotNull( p );
		assertTrue( p == net_client.getProcedureManager().getRegistered( info2 ) );
		assertTrue( net_client.getHome().hasProcedure( info1 ) );
		assertTrue( net_client.getHome().hasProcedure( info2 ) );
		
		assertNotNull( client_on_router );
		assertTrue(client_on_router.awaitProcedure( info1, 1, TimeUnit.SECONDS ));
		assertTrue( client_on_router.getProcedures().size() > 0 );
		assertTrue( client_on_router.hasProcedure( info1 ) );
		assertTrue( client_on_router.hasProcedure( info2 ) );
		
		CallableProcedure<Void, String> p1 = info1.bind( net_router, func );
		CallableProcedure<Void, String> p2 = info1.bind( net_router, Void.class, String.class );
		assertEquals( p1, p2 );
		
		assertEquals( 0, runCount.get() );
		SingleProcedureCall<Void, String> call = p1.call( client_on_router, null );
		assertEquals( helloWorld, call.getResult().get( 1, TimeUnit.MILLISECONDS ) );
		assertEquals( 1, runCount.get() );
		
	}
	
	@Test
	public void testMulti() throws InterruptedException, TimeoutException, ExecutionException {
				
		String helloWorld = "Hello World!";
		final AtomicInteger runCount = new AtomicInteger( 0 );
		Function<String, String> func = ( t ) -> {
			runCount.incrementAndGet();
			return t;
		};
		

		Procedure info = Procedure.of( "testMulti", String.class, String.class );
		
		
		net_router.getProcedureManager().register( "testMulti", func );
		net_client.getProcedureManager().register( "testMulti", func ).getRegisterFuture().await( 1000 );
		

		assertTrue(net_client.getNode( net_router.getHome().getId() ).awaitProcedure( info, 1, TimeUnit.SECONDS ));
		assertTrue(net_router.getNode( net_client.getHome().getId() ).awaitProcedure( info, 1, TimeUnit.SECONDS ));
		
		
		CallableProcedure<String, String> p = info.bind( net_router, func );
		
		MultiProcedureCall<String, String> call = p.call( Target.toAll(), helloWorld );
		call.await( 1, TimeUnit.SECONDS );
		ProcedureCallResult<String, String> rf = call.getResults().get( net_router.getHome().getId() );
		ProcedureCallResult<String, String> cf = call.getResults().get( net_client.getHome().getId() );
		assertNotNull( rf );
		assertNotNull( cf );
		assertTrue( rf.isDone() );
		assertTrue( cf.isDone() );
		assertEquals( helloWorld, rf.get() );
		assertEquals( helloWorld, cf.get() );
		
		
	}
}
