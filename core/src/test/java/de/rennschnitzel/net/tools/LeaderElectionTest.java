package de.rennschnitzel.net.tools;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;

import de.rennschnitzel.net.ProtocolUtils;
import de.rennschnitzel.net.core.Node;
import de.rennschnitzel.net.dummy.DummClientNetwork;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeMessage;
import de.rennschnitzel.net.service.LeaderElection;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;

public class LeaderElectionTest {
	
	EventLoopGroup group;
	
	long timestamp = System.currentTimeMillis();
	
	@Before
	public void setup() {
		group = new DefaultEventLoopGroup();
	}
	
	@After
	public void tearDown() {
		group.shutdownGracefully( 1, 100, TimeUnit.MILLISECONDS );
	}
	
	@Test
	public void testElection() throws Exception {
		
		System.out.println( new UUID( 0, 10 ).compareTo( new UUID( 0, 9 ) ) >0 );
		
		DummClientNetwork network = new DummClientNetwork( group, new UUID( 0, 10 ) );
		
		LeaderElection elect = new LeaderElection( network, "test", 10 );
		assertNull( elect.getLeader() );
		
		elect.register();
		assertNull( elect.getLeader() );
		Thread.sleep( 10 );
		assertTrue( elect.isLeader() );
		assertEquals( network.getHome(), elect.getLeader() );
		
		network.updateNode( null, createNodeMessage( new UUID( 0, 9 ), NodeMessage.Type.BUKKIT, Collections.emptySet() ) );
		assertTrue( elect.isLeader() );
		assertEquals( network.getHome(), elect.getLeader() );
		network.updateNode( null, createNodeMessage( new UUID( 0, 9 ), NodeMessage.Type.BUKKIT, Sets.newHashSet( elect.getNamespace().getName() ) ) );
		assertNull( elect.getLeader() );
		assertFalse( elect.isLeader() );
		Thread.sleep( 10 );
		assertTrue( elect.isLeader( new UUID( 0, 9 ) ) );
		
		network.updateNode( null, createNodeMessage( new UUID( 0, 11 ), NodeMessage.Type.BUKKIT, Collections.emptySet() ) );
		assertTrue( elect.isLeader( new UUID( 0, 9 ) ) );
		network.updateNode( null, createNodeMessage( new UUID( 0, 11 ), NodeMessage.Type.BUKKIT, Sets.newHashSet( elect.getNamespace().getName() ) ) );
		assertTrue( elect.isLeader( new UUID( 0, 9 ) ) );
		

		network.updateNode( null, createNodeMessage( new UUID( 0, 8 ), NodeMessage.Type.BUKKIT, Sets.newHashSet( elect.getNamespace().getName() ) ) );
		assertNull( elect.getLeader() );
		assertFalse( elect.isLeader() );
		Thread.sleep( 10 );
		assertTrue( elect.isLeader( new UUID( 0, 8 ) ) );
		
		
	}
	
	private NodeMessage createNodeMessage( UUID id, NodeMessage.Type type, Set<String> namespaces ) {
		NodeMessage.Builder b = NodeMessage.newBuilder();
		b.setType( type );
		new Node( id ).getData().put( b );
		b.setId( ProtocolUtils.convert( id ) );
		b.setStartTimestamp( timestamp );
		b.addAllNamespaces( namespaces );
		return b.build();
	}
	
}
