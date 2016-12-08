package de.mickare.routernet.util.collection;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.function.Consumer;

import de.mickare.routernet.util.concurrent.CloseableLock;
import de.mickare.routernet.util.concurrent.ReentrantCloseableReadWriteLock;

public class ConditionList<E> extends ConditionCollection<E>implements List<E> {
	
	public static <E> ConditionList<E> of( List<E> delegate ) {
		return new ConditionList<>( delegate );
	}
	
	private final List<E> delegate;
	private final ReentrantCloseableReadWriteLock lock;
	private final Condition condition;
	
	public ConditionList( List<E> delegate ) {
		this( delegate, new ReentrantCloseableReadWriteLock() );
	}
	
	protected ConditionList( List<E> delegate, ReentrantCloseableReadWriteLock lock ) {
		this( delegate, lock, lock.writeLock().newCondition() );
	}
	
	protected ConditionList( List<E> delegate, ReentrantCloseableReadWriteLock lock, Condition condition ) {
		super( delegate, lock, condition );
		this.delegate = delegate;
		this.lock = lock;
		this.condition = condition;
	}
	

	public void safeList( Consumer<List<E>> safe ) {
		try ( CloseableLock l = lock.writeLock().open() ) {
			safe.accept( this );
		}
	}
	
	@Override
	public boolean addAll( int index, Collection<? extends E> c ) {
		try ( CloseableLock l = lock.writeLock().open() ) {
			boolean result = delegate.addAll( index, c );
			condition.signalAll();
			return result;
		}
	}
	
	@Override
	public E get( int index ) {
		try ( CloseableLock l = lock.readLock().open() ) {
			return delegate.get( index );
		}
	}
	
	@Override
	public E set( int index, E element ) {
		try ( CloseableLock l = lock.writeLock().open() ) {
			E result = delegate.set( index, element );
			condition.signalAll();
			return result;
		}
	}
	
	@Override
	public void add( int index, E element ) {
		try ( CloseableLock l = lock.writeLock().open() ) {
			delegate.add( index, element );
			condition.signalAll();
		}
	}
	
	@Override
	public E remove( int index ) {
		try ( CloseableLock l = lock.writeLock().open() ) {
			E result = delegate.remove( index );
			condition.signalAll();
			return result;
		}
	}
	
	@Override
	public int indexOf( Object o ) {
		try ( CloseableLock l = lock.readLock().open() ) {
			return delegate.indexOf( o );
		}
	}
	
	@Override
	public int lastIndexOf( Object o ) {
		try ( CloseableLock l = lock.readLock().open() ) {
			return delegate.lastIndexOf( o );
		}
	}
	
	@Override
	public ConditionListIterator<E> listIterator() {
		try ( CloseableLock l = lock.readLock().open() ) {
			return new ConditionListIterator<>( delegate.listIterator(), this.lock, this.condition );
		}
	}
	
	@Override
	public ConditionListIterator<E> listIterator( int index ) {
		try ( CloseableLock l = lock.readLock().open() ) {
			return new ConditionListIterator<>( delegate.listIterator( index ), this.lock, this.condition );
		}
	}
	
	@Override
	public ConditionList<E> subList( int fromIndex, int toIndex ) {
		try ( CloseableLock l = lock.readLock().open() ) {
			return new ConditionList<>( delegate.subList( fromIndex, toIndex ), this.lock, this.condition );
		}
	}
	
}
