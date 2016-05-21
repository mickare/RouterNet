package de.rennschnitzel.net.util.collection;

import java.util.Set;
import java.util.concurrent.locks.Condition;

import de.rennschnitzel.net.util.concurrent.ReentrantCloseableReadWriteLock;

public class ConditionSet<E> extends ConditionCollection<E>implements Set<E> {
	
	public static <E> ConditionSet<E> of( Set<E> delegate ) {
		return new ConditionSet<>( delegate );
	}
	
	public ConditionSet( Set<E> delegate ) {
		this( delegate, new ReentrantCloseableReadWriteLock() );
	}
	
	protected ConditionSet( Set<E> delegate, ReentrantCloseableReadWriteLock lock ) {
		this( delegate, lock, lock.writeLock().newCondition() );
	}
	
	protected ConditionSet( Set<E> delegate, ReentrantCloseableReadWriteLock lock, Condition condition ) {
		super( delegate, lock, condition );
	}
	
}
