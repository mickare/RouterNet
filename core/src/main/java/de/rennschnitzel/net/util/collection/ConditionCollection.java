package de.rennschnitzel.net.util.collection;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import de.rennschnitzel.net.util.concurrent.CloseableLock;
import de.rennschnitzel.net.util.concurrent.ReentrantCloseableReadWriteLock;

public class ConditionCollection<E> implements Collection<E> {
	
	public static <E> ConditionCollection<E> of( Collection<E> delegate ) {
		return new ConditionCollection<>( delegate );
	}
	
	private final Collection<E> delegate;
	private final ReentrantCloseableReadWriteLock lock;
	private final Condition condition;
	
	public ConditionCollection( Collection<E> delegate ) {
		this( delegate, new ReentrantCloseableReadWriteLock() );
	}
	
	protected ConditionCollection( Collection<E> delegate, ReentrantCloseableReadWriteLock lock ) {
		this( delegate, lock, lock.writeLock().newCondition() );
	}
	
	protected ConditionCollection( Collection<E> delegate, ReentrantCloseableReadWriteLock lock, Condition condition ) {
		Preconditions.checkNotNull( delegate );
		Preconditions.checkNotNull( lock );
		Preconditions.checkNotNull( condition );
		this.delegate = delegate;
		this.lock = lock;
		this.condition = condition;
	}
	
	public void awaitContains( Object o ) throws InterruptedException {
		awaitCondition( c -> c.contains( o ) );
	}
	
	public boolean awaitContains( Object o, long time, TimeUnit unit ) throws InterruptedException {
		return awaitCondition( c -> c.contains( o ), time, unit );
	}
	
	public void awaitCondition( Predicate<ConditionCollection<E>> awaiting ) throws InterruptedException {
		try ( CloseableLock l = lock.writeLock().open() ) {
			while ( !awaiting.test( this ) ) {
				condition.await();
			}
		}
	}
	
	public boolean awaitCondition( Predicate<ConditionCollection<E>> awaiting, long time, TimeUnit unit ) throws InterruptedException {
		try ( CloseableLock l = lock.writeLock().open() ) {
			boolean waiting = true;
			boolean awaited = false;
			while ( !( awaited = awaiting.test( this ) ) && waiting ) {
				waiting = condition.await( time, unit );
			}
			return awaited;
		}
	}
	
	public void safe( Consumer<Collection<E>> safe ) {
		try ( CloseableLock l = lock.writeLock().open() ) {
			safe.accept( this );
		}
	}
	
	@Override
	public int size() {
		try ( CloseableLock l = lock.readLock().open() ) {
			return delegate.size();
		}
	}
	
	@Override
	public boolean isEmpty() {
		try ( CloseableLock l = lock.readLock().open() ) {
			return delegate.isEmpty();
		}
	}
	
	@Override
	public boolean contains( Object o ) {
		try ( CloseableLock l = lock.readLock().open() ) {
			return delegate.contains( o );
		}
	}
	
	public ImmutableSet<E> immutable() {
		try ( CloseableLock l = lock.readLock().open() ) {
			return ImmutableSet.copyOf( delegate );
		}
	}
	
	@Override
	@Deprecated
	public ConditionIterator<E> iterator() {
		try ( CloseableLock l = lock.readLock().open() ) {
			return new ConditionIterator<>( delegate.iterator(), this.lock, this.condition );
		}
	}
	
	@Override
	public Object[] toArray() {
		try ( CloseableLock l = lock.readLock().open() ) {
			return delegate.toArray();
		}
	}
	
	@Override
	public <T> T[] toArray( T[] a ) {
		try ( CloseableLock l = lock.readLock().open() ) {
			return delegate.toArray( a );
		}
	}
	
	@Override
	public boolean add( E e ) {
		try ( CloseableLock l = lock.writeLock().open() ) {
			boolean result = delegate.add( e );
			condition.signalAll();
			return result;
		}
	}
	
	@Override
	public boolean remove( Object o ) {
		try ( CloseableLock l = lock.writeLock().open() ) {
			boolean result = delegate.remove( o );
			condition.signalAll();
			return result;
		}
	}
	
	@Override
	public boolean containsAll( Collection<?> c ) {
		try ( CloseableLock l = lock.readLock().open() ) {
			boolean result = delegate.containsAll( c );
			condition.signalAll();
			return result;
		}
	}
	
	@Override
	public boolean addAll( Collection<? extends E> c ) {
		try ( CloseableLock l = lock.writeLock().open() ) {
			boolean result = delegate.addAll( c );
			condition.signalAll();
			return result;
		}
	}
	
	@Override
	public boolean retainAll( Collection<?> c ) {
		try ( CloseableLock l = lock.writeLock().open() ) {
			boolean result = delegate.retainAll( c );
			condition.signalAll();
			return result;
		}
	}
	
	@Override
	public boolean removeAll( Collection<?> c ) {
		try ( CloseableLock l = lock.writeLock().open() ) {
			boolean result = delegate.removeAll( c );
			condition.signalAll();
			return result;
		}
	}
	
	@Override
	public void clear() {
		try ( CloseableLock l = lock.writeLock().open() ) {
			delegate.clear();
			condition.signalAll();
		}
	}
	
}
