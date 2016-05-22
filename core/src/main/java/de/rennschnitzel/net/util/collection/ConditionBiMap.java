package de.rennschnitzel.net.util.collection;

import java.util.concurrent.locks.Condition;
import java.util.function.Consumer;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

import de.rennschnitzel.net.util.concurrent.CloseableLock;
import de.rennschnitzel.net.util.concurrent.ReentrantCloseableReadWriteLock;

public class ConditionBiMap<K, V> extends ConditionMap<K, V>implements BiMap<K, V> {
	
	public static <K, V> ConditionBiMap<K, V> of( BiMap<K, V> delegate ) {
		return new ConditionBiMap<>( delegate );
	}
	
	private final ReentrantCloseableReadWriteLock lock;
	private final Condition condition;
	
	private final ConditionSet<V> values;
	private final ConditionBiMap<V, K> inverse;
	
	public ConditionBiMap( BiMap<K, V> delegate ) {
		this( delegate, new ReentrantCloseableReadWriteLock() );
	}
	
	protected ConditionBiMap( BiMap<K, V> delegate, ReentrantCloseableReadWriteLock lock ) {
		this( delegate, lock, lock.writeLock().newCondition() );
	}
	
	protected ConditionBiMap( BiMap<K, V> delegate, ReentrantCloseableReadWriteLock lock, Condition condition ) {
		super( delegate, lock, condition );
		this.lock = lock;
		this.condition = condition;
		this.values = new ConditionSet<>( delegate.values() );
		this.inverse = new ConditionBiMap<V, K>( this, delegate.inverse(), lock, condition );
	}
	
	// inverse
	private ConditionBiMap( ConditionBiMap<V, K> parent, BiMap<K, V> inverse, ReentrantCloseableReadWriteLock lock, Condition condition ) {
		super( inverse, lock, condition );
		this.lock = lock;
		this.condition = condition;
		this.values = new ConditionSet<>( inverse.values() );
		this.inverse = parent;
	}
	
	public void safeBiMap( Consumer<BiMap<K, V>> safe ) {
		try ( CloseableLock l = lock.writeLock().open() ) {
			safe.accept( this );
		}
	}
	
	public ImmutableBiMap<K, V> immutable() {
		try ( CloseableLock l = lock.readLock().open() ) {
			return ImmutableBiMap.copyOf( delegate );
		}
	}
	
	@Override
	public V forcePut( K key, V value ) {
		try ( CloseableLock l = lock.writeLock().open() ) {
			V result = delegate.put( key, value );
			condition.signalAll();
			return result;
		}
	}
	
	@Override
	public BiMap<V, K> inverse() {
		return inverse;
	}
	
	@Override
	public ConditionSet<V> values() {
		return this.values;
	}
	
}
