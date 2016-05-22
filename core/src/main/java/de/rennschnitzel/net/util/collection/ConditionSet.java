package de.rennschnitzel.net.util.collection;

import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.function.Consumer;

import de.rennschnitzel.net.util.concurrent.CloseableLock;
import de.rennschnitzel.net.util.concurrent.ReentrantCloseableReadWriteLock;

public class ConditionSet<E> extends ConditionCollection<E>implements Set<E> {
	
	public static <E> ConditionSet<E> of( Set<E> delegate ) {
		return new ConditionSet<>( delegate );
	}
	
	private final ReentrantCloseableReadWriteLock lock;
	
	public ConditionSet( Set<E> delegate ) {
		this( delegate, new ReentrantCloseableReadWriteLock() );
	}
	
	protected ConditionSet( Set<E> delegate, ReentrantCloseableReadWriteLock lock ) {
		this( delegate, lock, lock.writeLock().newCondition() );
	}
	
	protected ConditionSet( Set<E> delegate, ReentrantCloseableReadWriteLock lock, Condition condition ) {
		super( delegate, lock, condition );
		this.lock = lock;
	}
	
	public void safeSet( Consumer<Set<E>> safe ) {
		try ( CloseableLock l = lock.writeLock().open() ) {
			safe.accept( this );
		}
	}
	
}
