package de.mickare.routernet.util.collection;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import de.mickare.routernet.util.concurrent.CloseableLock;
import de.mickare.routernet.util.concurrent.ReentrantCloseableReadWriteLock;

public class ConditionMap<K, V> implements Map<K, V> {
	
	public static <K, V> ConditionMap<K, V> of( Map<K, V> delegate ) {
		return new ConditionMap<>( delegate );
	}
	
	protected final Map<K, V> delegate;
	private final ReentrantCloseableReadWriteLock lock;
	private final Condition condition;
	
	private final ConditionSet<K> keySet;
	private final ConditionCollection<V> values;
	private final ConditionSet<Entry<K, V>> entrySet;
	
	public ConditionMap( Map<K, V> delegate ) {
		this( delegate, new ReentrantCloseableReadWriteLock() );
	}
	
	protected ConditionMap( Map<K, V> delegate, ReentrantCloseableReadWriteLock lock ) {
		this( delegate, lock, lock.writeLock().newCondition() );
	}
	
	public ConditionMap( Map<K, V> delegate, ReentrantCloseableReadWriteLock lock, Condition condition ) {
		Preconditions.checkNotNull( delegate );
		Preconditions.checkNotNull( lock );
		Preconditions.checkNotNull( condition );
		this.delegate = delegate;
		this.lock = lock;
		this.condition = condition;
		this.keySet = new ConditionSet<>( delegate.keySet(), this.lock, this.condition );
		this.entrySet = new ConditionSet<>( delegate.entrySet(), this.lock, this.condition );
		this.values = new ConditionCollection<>( delegate.values(), this.lock, this.condition );
	}
	
	public void safe( Consumer<Map<K, V>> safe ) {
		try ( CloseableLock l = lock.writeLock().open() ) {
			safe.accept( this );
		}
	}
	
	public ImmutableMap<K, V> immutable() {
		try ( CloseableLock l = lock.readLock().open() ) {
			return ImmutableMap.copyOf( delegate );
		}
	}
	
	public void awaitContainsKey( Object o ) throws InterruptedException {
		awaitCondition( c -> c.containsKey( o ) );
	}
	
	public boolean awaitContainsKey( Object o, long time, TimeUnit unit ) throws InterruptedException {
		return awaitCondition( c -> c.containsKey( o ), time, unit );
	}
	
	public void awaitContainsValue( Object o ) throws InterruptedException {
		awaitCondition( c -> c.containsValue( o ) );
	}
	
	public boolean awaitContainsValue( Object o, long time, TimeUnit unit ) throws InterruptedException {
		return awaitCondition( c -> c.containsValue( o ), time, unit );
	}
	
	public void awaitCondition( Predicate<ConditionMap<K, V>> awaiting ) throws InterruptedException {
		try ( CloseableLock l = lock.writeLock().open() ) {
			while ( !awaiting.test( this ) ) {
				condition.await();
			}
		}
	}
	
	public boolean awaitCondition( Predicate<ConditionMap<K, V>> awaiting, long time, TimeUnit unit ) throws InterruptedException {
		try ( CloseableLock l = lock.writeLock().open() ) {
			boolean waiting = true;
			boolean awaited = false;
			while ( !( awaited = awaiting.test( this ) ) && waiting ) {
				waiting = condition.await( time, unit );
			}
			return awaited;
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
	public boolean containsKey( Object key ) {
		try ( CloseableLock l = lock.readLock().open() ) {
			return delegate.containsKey( key );
		}
	}
	
	@Override
	public boolean containsValue( Object value ) {
		try ( CloseableLock l = lock.readLock().open() ) {
			return delegate.containsValue( value );
		}
	}
	
	@Override
	public V get( Object key ) {
		try ( CloseableLock l = lock.readLock().open() ) {
			return delegate.get( key );
		}
	}
	
	@Override
	public V put( K key, V value ) {
		try ( CloseableLock l = lock.writeLock().open() ) {
			V result = delegate.put( key, value );
			condition.signalAll();
			return result;
		}
	}
	
	@Override
	public V remove( Object key ) {
		try ( CloseableLock l = lock.writeLock().open() ) {
			V result = delegate.remove( key );
			condition.signalAll();
			return result;
		}
	}
	
	@Override
	public void putAll( Map<? extends K, ? extends V> m ) {
		try ( CloseableLock l = lock.writeLock().open() ) {
			delegate.putAll( m );
			condition.signalAll();
		}
	}
	
	@Override
	public void clear() {
		try ( CloseableLock l = lock.writeLock().open() ) {
			delegate.clear();
			condition.signalAll();
		}
	}
	
	@Override
	public ConditionSet<K> keySet() {
		return this.keySet;
	}
	
	@Override
	public ConditionCollection<V> values() {
		return this.values;
	}
	
	@Override
	public ConditionSet<Entry<K, V>> entrySet() {
		return this.entrySet;
	}
	
}
