package de.rennschnitzel.net.util.collection;

import static org.junit.Assert.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class TestConditionCollections {
	
	private ScheduledExecutorService pool;
	
	@Before
	public void setup() {
		pool = Executors.newScheduledThreadPool( Runtime.getRuntime().availableProcessors() );
	}
	
	@After
	public void tearDown() {
		pool.shutdown();
	}
	
	@Test
	public void testCollection() throws Exception {
		
		final ConditionCollection<Integer> c = new ConditionCollection<>( Sets.newHashSet() );
		
		assertTrue( c.add( 2 ) );
		assertFalse( c.add( 2 ) );
		assertTrue( c.awaitContains( 2, 1, TimeUnit.MILLISECONDS ) );
		
		pool.schedule( () -> c.add( 3 ), 5, TimeUnit.MILLISECONDS );
		assertTrue( c.awaitContains( 3, 100, TimeUnit.MILLISECONDS ) );
		
		assertFalse( c.remove( 1 ) );
		assertTrue( c.remove( 2 ) );
		assertFalse( c.awaitContains( 2, 1, TimeUnit.MILLISECONDS ) );
		
	}
	
	@Test
	public void testMap() throws Exception {
		
		final ConditionMap<Integer, String> c = new ConditionMap<>( Maps.newHashMap() );
		
		assertNull( c.put( 1, "a" ) );
		assertEquals( "a", c.put( 1, "b" ) );
		assertTrue( c.awaitContainsKey( 1, 1, TimeUnit.MILLISECONDS ) );
		assertTrue( c.awaitContainsValue( "b", 1, TimeUnit.MILLISECONDS ) );
		
		pool.schedule( () -> c.put( 2, "c" ), 5, TimeUnit.MILLISECONDS );
		pool.schedule( () -> c.put( 2, "d" ), 10, TimeUnit.MILLISECONDS );
		assertTrue( c.awaitContainsKey( 2, 100, TimeUnit.MILLISECONDS ) );
		assertTrue( c.awaitContainsValue( "d", 100, TimeUnit.MILLISECONDS ) );
		
		assertEquals( "b", c.remove( 1 ) );
		assertEquals( "d", c.remove( 2 ) );
		assertFalse( c.awaitContainsKey( 1, 1, TimeUnit.MILLISECONDS ) );
		assertFalse( c.awaitContainsKey( 2, 1, TimeUnit.MILLISECONDS ) );
		
	}
	
}
