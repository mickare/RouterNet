package de.rennschnitzel.net.core.procedure;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import de.rennschnitzel.net.core.AbstractNetwork;
import lombok.Data;
import lombok.NonNull;

public class OpenCallsCache {
	
	private final @NonNull AbstractNetwork network;
	private final @NonNull ScheduledExecutorService executor;
	private final Cache<Integer, Entry> openCalls;
	
	public OpenCallsCache( AbstractNetwork network, ScheduledExecutorService executor, long max_timeout ) {
		Preconditions.checkNotNull( network );
		Preconditions.checkNotNull( executor );
		Preconditions.checkArgument( max_timeout > 0 );
		this.network = network;
		this.executor = executor;
		
		openCalls = CacheBuilder.newBuilder()//
				.expireAfterWrite( max_timeout, TimeUnit.MILLISECONDS )//
				.removalListener( new RemovalListener<Integer, Entry>() {
					@Override
					public void onRemoval( RemovalNotification<Integer, Entry> notify ) {
						notify.getValue().timeoutTask.cancel( false );
						notify.getValue().call.checkTimeout();
					}
				} )//
				.build();
		
	}
	
	private static @Data class Entry {
		private final @NonNull ProcedureCall<?, ?> call;
		private final @NonNull Future<?> timeoutTask;
	}
	
	private Future<?> schedule( final ProcedureCall<?, ?> call ) {
		long timeout = call.getRemainingTimeout();
		if ( timeout <= 0 ) {
			call.checkTimeout();
			return CompletableFuture.completedFuture( call );
		}
		return executor.schedule( () -> call.checkTimeout(), call.getRemainingTimeout() + 1, TimeUnit.MILLISECONDS );
	}
	
	public void put( ProcedureCall<?, ?> call ) {
		call.checkTimeout();
		if ( !call.isDone() ) {
			final int id = call.getId();
			this.openCalls.put( id, new Entry( call, schedule( call ) ) );
			call.addListener( c -> {
				invalidate( id );
			} );
		}
	}
	
	public void invalidate( int id ) {
		this.openCalls.invalidate( id );
	}
	
	public ProcedureCall<?, ?> get( int id ) {
		Entry e = openCalls.getIfPresent( id );
		return e != null ? e.call : null;
	}
	
}
