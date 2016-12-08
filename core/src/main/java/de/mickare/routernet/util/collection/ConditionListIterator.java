package de.mickare.routernet.util.collection;

import java.util.ListIterator;
import java.util.concurrent.locks.Condition;

import de.mickare.routernet.util.concurrent.CloseableLock;
import de.mickare.routernet.util.concurrent.ReentrantCloseableReadWriteLock;

public class ConditionListIterator<E> extends ConditionIterator<E>implements ListIterator<E> {
	
	private final ListIterator<E> delegate;
	private final ReentrantCloseableReadWriteLock lock;
	private final Condition condition;
	
	public ConditionListIterator( ListIterator<E> delegate ) {
		this( delegate, new ReentrantCloseableReadWriteLock() );
	}
	
	protected ConditionListIterator( ListIterator<E> delegate, ReentrantCloseableReadWriteLock lock ) {
		this( delegate, lock, lock.writeLock().newCondition() );
	}
	
	protected ConditionListIterator( ListIterator<E> delegate, ReentrantCloseableReadWriteLock lock, Condition condition ) {
		super( delegate, lock, condition );
		this.delegate = delegate;
		this.lock = lock;
		this.condition = condition;
	}
	
	@Override
	public boolean hasPrevious() {
		try ( CloseableLock l = lock.readLock().open() ) {
			return delegate.hasPrevious();
		}
	}
	
	@Override
	public E previous() {
		try ( CloseableLock l = lock.readLock().open() ) {
			return delegate.previous();
		}
	}
	
	@Override
	public int nextIndex() {
		try ( CloseableLock l = lock.readLock().open() ) {
			return delegate.nextIndex();
		}
	}
	
	@Override
	public int previousIndex() {
		try ( CloseableLock l = lock.readLock().open() ) {
			return delegate.previousIndex();
		}
	}
	
	@Override
	public void set( E e ) {
		try ( CloseableLock l = lock.writeLock().open() ) {
			delegate.set( e );
			condition.signalAll();
		}
	}
	
	@Override
	public void add( E e ) {
		try ( CloseableLock l = lock.writeLock().open() ) {
			delegate.add( e );
			condition.signalAll();
		}
	}
	
}
