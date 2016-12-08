package de.mickare.routernet.util.concurrent;

import java.util.concurrent.locks.ReadWriteLock;

/**
 * 
 * @author mickare
 *
 */
public interface CloseableReadWriteLock extends ReadWriteLock {
	
	public CloseableLock readLock();
	
	public CloseableLock writeLock();
	
}
