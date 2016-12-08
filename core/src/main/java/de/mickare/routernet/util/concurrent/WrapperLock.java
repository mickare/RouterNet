package de.mickare.routernet.util.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * 
 * @author mickare
 *
 */
public class WrapperLock implements CloseableLock {
	
	private final Lock wrapped;
	
	public WrapperLock( Lock wrapped ) {
		this.wrapped = wrapped;
	}
	
	@Override
	public WrapperLock open() {
		this.lock();
		return this;
	}
	
	@Override
	public WrapperLock open( long time, TimeUnit unit ) throws TimeoutException, InterruptedException {
		if ( !this.tryLock( time, unit ) ) {
			throw new TimeoutException();
		}
		return this;
	}
	
	@Override
	public void close() {
		this.unlock();
	}
	
	@Override
	public void lock() {
		wrapped.lock();
	}
	
	@Override
	public void lockInterruptibly() throws InterruptedException {
		wrapped.lockInterruptibly();
	}
	
	@Override
	public boolean tryLock() {
		return wrapped.tryLock();
	}
	
	@Override
	public boolean tryLock( long time, TimeUnit unit ) throws InterruptedException {
		return wrapped.tryLock( time, unit );
	}
	
	@Override
	public void unlock() {
		wrapped.unlock();
	}
	
	@Override
	public Condition newCondition() {
		return wrapped.newCondition();
	}
	
}
