package de.rennschnitzel.net.util.concurrent;

import java.io.Closeable;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public class ResourcePool<R> {
	
	private @Getter boolean closed = false;
	private final @Getter int capacity;
	private final Supplier<R> defaultSupplier;
	private final ArrayBlockingQueue<R> queue;
	private final Set<R> elements;
	
	public ResourcePool( int capacity ) {
		this( capacity, null );
	}
	
	public ResourcePool( int capacity, Supplier<R> defaultSupplier ) {
		Preconditions.checkArgument( capacity > 0 );
		this.capacity = capacity;
		this.defaultSupplier = defaultSupplier;
		this.queue = new ArrayBlockingQueue<>( capacity, true );
		this.elements = Collections.synchronizedSet( Sets.newIdentityHashSet() );
	}
	
	private void checkClosed() {
		if ( closed ) {
			throw new RuntimeException( "resource pool closed" );
		}
	}
	
	public R get() throws InterruptedException {
		return get( this.defaultSupplier );
	}
	
	public R get( long timeout, TimeUnit unit ) throws InterruptedException, TimeoutException {
		return get( this.defaultSupplier );
	}
	
	public R get( Supplier<R> supplier ) throws InterruptedException {
		Preconditions.checkNotNull( supplier );
		checkClosed();
		R resource = queue.poll();
		if ( resource == null ) {
			if ( elements.size() == capacity ) {
				return queue.take();
			} else {
				synchronized ( this.elements ) {
					if ( elements.size() < capacity ) {
						resource = supplier.get();
						Preconditions.checkNotNull( resource );
						if ( !elements.add( resource ) ) {
							throw new IllegalStateException( "supplier didn't return new instance" );
						}
						return resource;
					}
				}
				return queue.take();
			}
		}
		return resource;
	}
	
	public R get( Supplier<R> supplier, long timeout, TimeUnit unit ) throws InterruptedException, TimeoutException {
		Preconditions.checkNotNull( supplier );
		checkClosed();
		R resource = queue.poll();
		if ( resource == null ) {
			if ( elements.size() == capacity ) {
				resource = queue.poll( timeout, unit );
				if ( resource == null ) {
					throw new TimeoutException();
				}
			} else {
				synchronized ( this.elements ) {
					if ( elements.size() < capacity ) {
						resource = supplier.get();
						Preconditions.checkNotNull( resource );
						if ( !elements.add( resource ) ) {
							throw new IllegalStateException( "supplier didn't return new instance" );
						}
						return resource;
					}
				}
				resource = queue.poll( timeout, unit );
				if ( resource == null ) {
					throw new TimeoutException();
				}
			}
		}
		return resource;
	}
	
	public Entry getEntry() throws InterruptedException {
		return new Entry( get() );
	}
	
	public Entry getEntry( long timeout, TimeUnit unit ) throws InterruptedException, TimeoutException {
		return new Entry( get( timeout, unit ) );
	}
	
	public Entry getEntry( Supplier<R> supplier ) throws InterruptedException {
		return new Entry( get( supplier ) );
	}
	
	public Entry getEntry( Supplier<R> supplier, long timeout, TimeUnit unit ) throws InterruptedException, TimeoutException {
		return new Entry( get( supplier, timeout, unit ) );
	}
	
	public void recycle( R resource ) {
		Preconditions.checkArgument( this.elements.contains( resource ) );
		synchronized ( this.queue ) {
			this.queue.remove( resource );
			this.queue.offer( resource );
		}
	}
	
	public void clear() {
		synchronized ( this.elements ) {
			this.elements.clear();
		}
		this.queue.clear();
	}
	
	public void close() {
		this.close( r -> {
			if ( r instanceof Closeable ) {
				try {
					( ( Closeable ) r ).close();
				} catch ( Exception e ) {
					e.printStackTrace();
				}
			}
		} );
	}
	
	public void close( Consumer<R> closer ) {
		this.closed = true;
		synchronized ( this.elements ) {
			if ( closer != null ) {
				this.elements.forEach( closer );
			}
			this.elements.clear();
		}
		this.queue.clear();
	}
	
	public void recycle( Entry entry ) {
		entry.close();
	}
	
	@RequiredArgsConstructor
	public class Entry implements AutoCloseable {
		
		private boolean closed = false;
		@NonNull
		@Getter
		private final R resource;
		
		@Override
		public synchronized void close() {
			if ( closed ) {
				return;
			}
			closed = true;
			ResourcePool.this.recycle( this.resource );
		}
	}
	
}
