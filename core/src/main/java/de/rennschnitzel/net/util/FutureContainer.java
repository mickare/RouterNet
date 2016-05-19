package de.rennschnitzel.net.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

public class FutureContainer<V> {
	
	public static enum State {
		UNSET,
		PROGRESS,
		FIXED;
	}
	
	private final SettableFuture<V> value = SettableFuture.create();
	
	private volatile State state = State.UNSET;
	
	public State state() {
		return state;
	}
	
	public V get() throws InterruptedException, ExecutionException {
		return value.get();
	}
	
	public V get( long timeout, TimeUnit unit ) throws InterruptedException, TimeoutException, ExecutionException {
		return value.get( timeout, unit );
	}
	
	public synchronized <X, Y extends V> boolean set( ListenableFuture<X> delegate, Function<X, Y> convert ) {
		if ( this.state != State.UNSET ) {
			return false;
		}
		return set( Futures.transform( delegate, convert ) );
	}
	
	public synchronized <Y extends V> boolean set( ListenableFuture<Y> delegate ) {
		if ( this.state != State.UNSET ) {
			return false;
		}
		this.state = State.PROGRESS;
		Futures.addCallback( delegate, new FutureCallback<Y>() {
			@Override
			public void onFailure( Throwable cause ) {
				FutureContainer.this.state = State.UNSET;
			}
			
			@Override
			public void onSuccess( Y value ) {
				if ( FutureContainer.this.value.set( value ) )
					FutureContainer.this.state = State.FIXED;
			}
		} );
		return true;
	}
	
	public void onSuccess( final Consumer<V> successHandler ) {
		Futures.addCallback( value, new FutureCallback<V>() {
			@Override
			public void onFailure( Throwable cause ) {
				
			}
			
			@Override
			public void onSuccess( V value ) {
				successHandler.accept( value );
			}
		} );
	}
	
}
