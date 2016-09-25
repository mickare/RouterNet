package de.mickare.net.core.procedure;

import java.util.function.Function;

import de.mickare.net.core.AbstractNetwork;
import io.netty.util.concurrent.Future;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class CallableRegisteredProcedure<T, R> extends BoundProcedure<T, R> {
	
	private transient @Getter @Setter @NonNull Future<?> registerFuture = null;
	private final @Getter boolean synchronization;
	
	public CallableRegisteredProcedure( AbstractNetwork network, CallableProcedure<T, R> procedure, Function<T, R> function, boolean synchronization ) {
		super( network, procedure, function );
		this.synchronization = synchronization;
	}
	
	public CallableRegisteredProcedure( AbstractNetwork network, Procedure procedure, Class<T> argClass, Class<R> resultClass, Function<T, R> function, boolean synchronization ) {
		super( network, procedure, argClass, resultClass, function );
		this.synchronization = synchronization;
	}
	
	public CallableRegisteredProcedure( AbstractNetwork network, String name, Class<T> argClass, Class<R> resultClass, Function<T, R> function, boolean synchronization ) {
		super( network, name, argClass, resultClass, function );
		this.synchronization = synchronization;
	}
	
}
