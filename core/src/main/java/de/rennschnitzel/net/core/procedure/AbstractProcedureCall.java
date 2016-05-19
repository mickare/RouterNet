package de.rennschnitzel.net.core.procedure;

import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.Target;
import de.rennschnitzel.net.protocol.TransportProtocol;
import lombok.Getter;

public abstract class AbstractProcedureCall<T, R> implements ProcedureCall<T, R> {
	
	private static final AtomicInteger ID_COUNTER = new AtomicInteger( new Random().nextInt() );
	
	private @Getter final int id = ID_COUNTER.incrementAndGet();
	private @Getter final long timestamp = System.currentTimeMillis();
	private @Getter final long maxTimeout; // in milliseconds
	private @Getter final CallableProcedure<T, R> procedure;
	private @Getter final Target target;
	private @Getter final T argument;
	
	/**
	 * 
	 * @param procedure
	 * @param target
	 * @param argument
	 * @param maxTimeout
	 *            in milliseconds
	 */
	public AbstractProcedureCall( CallableProcedure<T, R> procedure, Target target, T argument, long maxTimeout ) {
		Preconditions.checkNotNull( procedure );
		this.procedure = procedure;
		this.target = target;
		this.argument = argument;
		this.maxTimeout = maxTimeout;
	}
	
	public TransportProtocol.ProcedureCallMessage toProtocol() {
		TransportProtocol.ProcedureCallMessage.Builder b = TransportProtocol.ProcedureCallMessage.newBuilder();
		b.setProcedure( this.procedure.toProtocol() );
		b.setId( id );
		b.setTimestamp( timestamp );
		b.setMaxTimeout( maxTimeout );
		procedure.getCallWriter().accept( b, argument );
		return b.build();
	}
	
	@Override
	public boolean checkTimeout() {
		if ( this.getRemainingTimeout() <= 0 && !isDone() ) {
			setException( new TimeoutException() );
			return true;
		}
		return false;
	}
	
	@Override
	public void cancel() {
		if ( !this.isDone() ) {
			this.setException( new CancellationException() );
		}
	}
	
}
