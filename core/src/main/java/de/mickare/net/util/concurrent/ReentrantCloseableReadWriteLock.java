package de.mickare.net.util.concurrent;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 
 * @author mickare
 *
 */
public class ReentrantCloseableReadWriteLock implements CloseableReadWriteLock {
	
	private final CloseableLock readLock;
	private final CloseableLock writeLock;
	
	private final ReentrantReadWriteLock original;
	
	private ReentrantCloseableReadWriteLock( ReentrantReadWriteLock original ) {
		this.original = original;
		this.readLock = new WrapperLock( this.original.readLock() );
		this.writeLock = new WrapperLock( this.original.writeLock() );
	}
	
	public ReentrantCloseableReadWriteLock() {
		this( new ReentrantReadWriteLock() );
	}
	
	public ReentrantCloseableReadWriteLock( boolean fair ) {
		this( new ReentrantReadWriteLock( fair ) );
	}
	
	public CloseableLock readLock() {
		return this.readLock;
	}
	
	public CloseableLock writeLock() {
		return this.writeLock;
	}
	
}
