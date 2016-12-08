package de.mickare.routernet.util.collection;

import java.util.Iterator;
import java.util.concurrent.locks.Condition;

import com.google.common.base.Preconditions;

import de.mickare.routernet.util.concurrent.CloseableLock;
import de.mickare.routernet.util.concurrent.ReentrantCloseableReadWriteLock;

public class ConditionIterator<E> implements Iterator<E> {
	
	private final Iterator<E> delegate;
	private final ReentrantCloseableReadWriteLock lock;
	private final Condition condition;
	
	public ConditionIterator( Iterator<E> delegate ) {
		this( delegate, new ReentrantCloseableReadWriteLock() );
	}
	
	protected ConditionIterator( Iterator<E> delegate, ReentrantCloseableReadWriteLock lock ) {
		this( delegate, lock, lock.writeLock().newCondition() );
	}
	
	protected ConditionIterator( Iterator<E> delegate, ReentrantCloseableReadWriteLock lock, Condition condition ) {
		Preconditions.checkNotNull( delegate );
		Preconditions.checkNotNull( lock );
		Preconditions.checkNotNull( condition );
		this.delegate = delegate;
		this.lock = lock;
		this.condition = condition;
	}
	
	@Override
	public boolean hasNext() {
		try ( CloseableLock l = lock.readLock().open() ) {
			return delegate.hasNext();
		}
	}
	
	@Override
	public E next() {
		try ( CloseableLock l = lock.readLock().open() ) {
			return delegate.next();
		}
	}
	
	@Override
	public void remove() {
		try ( CloseableLock l = lock.writeLock().open() ) {
			delegate.remove();
			condition.signalAll();
		}
	}
	
}
