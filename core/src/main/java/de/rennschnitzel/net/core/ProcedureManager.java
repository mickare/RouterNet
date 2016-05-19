package de.rennschnitzel.net.core;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import de.rennschnitzel.net.ProtocolUtils;
import de.rennschnitzel.net.core.procedure.BoundProcedure;
import de.rennschnitzel.net.core.procedure.CallableProcedure;
import de.rennschnitzel.net.core.procedure.CallableRegisteredProcedure;
import de.rennschnitzel.net.core.procedure.MultiProcedureCall;
import de.rennschnitzel.net.core.procedure.OpenCallsCache;
import de.rennschnitzel.net.core.procedure.Procedure;
import de.rennschnitzel.net.core.procedure.ProcedureCall;
import de.rennschnitzel.net.core.procedure.SingleProcedureCall;
import de.rennschnitzel.net.exception.ProtocolException;
import de.rennschnitzel.net.protocol.TransportProtocol.ErrorMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureCallMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureResponseMessage;
import de.rennschnitzel.net.util.TypeUtils;
import de.rennschnitzel.net.util.concurrent.CloseableLock;
import de.rennschnitzel.net.util.concurrent.ReentrantCloseableReadWriteLock;
import io.netty.util.concurrent.Future;
import lombok.Getter;
import net.jodah.typetools.TypeResolver;

public class ProcedureManager {
	
	public static final boolean DEFAULT_SYNCHRONIZATION = true;
	
	/**
	 * Default timeout of procedures. (in milliseconds)
	 */
	public static long PROCEDURE_DEFAULT_TIMEOUT = 10 * 1000; // 10 seconds
	public static long MAX_TIMEOUT = 60 * 60 * 1000; // 1 hour
	
	private final ReentrantCloseableReadWriteLock lock = new ReentrantCloseableReadWriteLock();
	private final Map<Procedure, CallableRegisteredProcedure<?, ?>> registeredProcedures = Maps.newHashMap();
	private final @Getter AbstractNetwork network;
	
	private final OpenCallsCache openCalls;
	
	public ProcedureManager( AbstractNetwork network ) {
		Preconditions.checkNotNull( network );
		this.network = network;
		this.openCalls = new OpenCallsCache( network, MAX_TIMEOUT );
	}
	
	public <T, R> CallableRegisteredProcedure<T, R> register( String name, Function<T, R> function ) {
		return register( name, function, DEFAULT_SYNCHRONIZATION );
	}
	
	@SuppressWarnings( "unchecked" )
	public <T, R> CallableRegisteredProcedure<T, R> register( String name, Function<T, R> function, boolean synchronization ) {
		final Class<?>[] args = TypeUtils.resolveArgumentClass( function );
		return register( name, function, ( Class<T> ) args[0], ( Class<R> ) args[1], synchronization );
	}
	
	public <T> CallableRegisteredProcedure<T, Void> register( final String name, final Consumer<T> consumer ) {
		return register( name, consumer, DEFAULT_SYNCHRONIZATION );
	}
	
	public <T> CallableRegisteredProcedure<T, Void> register( final String name, final Consumer<T> consumer, boolean synchronization ) {
		return register( name, ( t ) -> {
			consumer.accept( t );
			return null;
		}, ( Class<T> ) TypeUtils.resolveArgumentClass( consumer ), Void.class, synchronization );
	}
	
	public <R> CallableRegisteredProcedure<Void, R> register( final String name, final Supplier<R> supplier ) {
		return register( name, supplier, DEFAULT_SYNCHRONIZATION );
	}
	
	public <R> CallableRegisteredProcedure<Void, R> register( final String name, final Supplier<R> supplier, boolean synchronization ) {
		return register( name, ( t ) -> supplier.get(), Void.class, ( Class<R> ) TypeUtils.resolveArgumentClass( supplier ), synchronization );
	}
	
	public CallableRegisteredProcedure<Void, Void> register( final String name, final Runnable run ) {
		return register( name, run, DEFAULT_SYNCHRONIZATION );
	}
	
	public CallableRegisteredProcedure<Void, Void> register( final String name, final Runnable run, boolean synchronization ) {
		return register( name, ( t ) -> {
			run.run();
			return null;
		}, Void.class, Void.class, synchronization );
	}
	
	public <T, R> CallableRegisteredProcedure<T, R> register( final String name, final Function<T, R> function, final Class<T> argClass, final Class<R> resultClass ) {
		return register( name, function, argClass, resultClass, DEFAULT_SYNCHRONIZATION );
	}
	
	public <T, R> CallableRegisteredProcedure<T, R> register( final String name, final Function<T, R> function, final Class<T> argClass, final Class<R> resultClass, boolean synchronization ) {
		Preconditions.checkNotNull( name );
		Preconditions.checkNotNull( function );
		Preconditions.checkArgument( !name.isEmpty() );
		Preconditions.checkNotNull( argClass );
		Preconditions.checkNotNull( resultClass );
		Preconditions.checkArgument( argClass != TypeResolver.Unknown.class );
		Preconditions.checkArgument( resultClass != TypeResolver.Unknown.class );
		final CallableRegisteredProcedure<T, R> proc = new CallableRegisteredProcedure<T, R>( network, name, argClass, resultClass, function, synchronization );
		proc.setRegisterFuture( _registerProcedure( proc ) );
		return proc;
	}
	
	public <T, R> CallableRegisteredProcedure<T, R> register( CallableProcedure<T, R> procedure, Function<T, R> function ) {
		return register( procedure, function, DEFAULT_SYNCHRONIZATION );
	}
	
	public <T, R> CallableRegisteredProcedure<T, R> register( CallableProcedure<T, R> procedure, Function<T, R> function, boolean synchronization ) {
		final CallableRegisteredProcedure<T, R> proc = new CallableRegisteredProcedure<>( network, procedure, function, synchronization );
		proc.setRegisterFuture( _registerProcedure( proc ) );
		return proc;
	}
	
	public <T, R> CallableRegisteredProcedure<T, R> register( BoundProcedure<T, R> procedure ) {
		return register( procedure, DEFAULT_SYNCHRONIZATION );
	}
	
	public <T, R> CallableRegisteredProcedure<T, R> register( BoundProcedure<T, R> procedure, boolean synchronization ) {
		final CallableRegisteredProcedure<T, R> proc = new CallableRegisteredProcedure<>( network, procedure, procedure.getFunction(), synchronization );
		proc.setRegisterFuture( _registerProcedure( proc ) );
		return proc;
	}
	
	private Future<?> _registerProcedure( CallableRegisteredProcedure<?, ?> proc ) {
		try ( CloseableLock l = lock.writeLock().open() ) {
			registeredProcedures.put( proc, proc );
		}
		network.getHome().addRegisteredProcedure( proc );
		return network.getHome().newUpdatePromise();
	}
	
	public CallableRegisteredProcedure<?, ?> getRegistered( Procedure info ) {
		try ( CloseableLock l = lock.readLock().open() ) {
			return this.registeredProcedures.get( info );
		}
	}
	
	public <R> SingleProcedureCall<Void, R> call( Node node, CallableProcedure<Void, R> procedure ) {
		return this.call( node, procedure, null );
	}
	
	public <R> SingleProcedureCall<Void, R> call( Node node, CallableProcedure<Void, R> procedure, long timeout ) {
		return this.call( node, procedure, null, timeout );
	}
	
	public <T, R> SingleProcedureCall<T, R> call( Node node, CallableProcedure<T, R> procedure, T argument ) {
		return this.call( node, procedure, argument, PROCEDURE_DEFAULT_TIMEOUT );
	}
	
	public <T, R> SingleProcedureCall<T, R> call( Node node, CallableProcedure<T, R> procedure, T argument, long timeout ) {
		Preconditions.checkNotNull( node );
		Preconditions.checkNotNull( procedure );
		Preconditions.checkArgument( timeout > 0 );
		final SingleProcedureCall<T, R> call = new SingleProcedureCall<>( node, procedure, argument, timeout );
		if ( !call.isDone() ) {
			openCalls.put( call );
			try {
				network.sendProcedureCall( call );
				if ( call.isDone() ) {
					openCalls.invalidate( call.getId() );
				}
			} catch ( Exception e ) {
				call.setException( e );
			}
		}
		return call;
	}
	
	public <R> MultiProcedureCall<Void, R> call( Target target, CallableProcedure<Void, R> procedure ) {
		return this.call( this.network.getNodes( target ), procedure, null );
	}
	
	public <R> MultiProcedureCall<Void, R> call( Target target, CallableProcedure<Void, R> procedure, long timeout ) {
		return this.call( this.network.getNodes( target ), procedure, null, timeout );
	}
	
	public <R> MultiProcedureCall<Void, R> call( Collection<Node> nodes, CallableProcedure<Void, R> procedure ) {
		return this.call( nodes, procedure, null );
	}
	
	public <R> MultiProcedureCall<Void, R> call( Collection<Node> nodes, CallableProcedure<Void, R> procedure, long timeout ) {
		return this.call( nodes, procedure, null, timeout );
	}
	
	public <T, R> MultiProcedureCall<T, R> call( Target target, CallableProcedure<T, R> procedure, T argument ) {
		return this.call( this.network.getNodes( target ), procedure, argument );
	}
	
	public <T, R> MultiProcedureCall<T, R> call( Target target, CallableProcedure<T, R> procedure, T argument, long timeout ) {
		return this.call( this.network.getNodes( target ), procedure, argument, timeout );
	}
	
	public <T, R> MultiProcedureCall<T, R> call( Collection<Node> nodes, CallableProcedure<T, R> procedure, T argument ) {
		return this.call( nodes, procedure, argument, PROCEDURE_DEFAULT_TIMEOUT );
	}
	
	public <T, R> MultiProcedureCall<T, R> call( Collection<Node> nodes, CallableProcedure<T, R> procedure, T argument, long timeout ) {
		Preconditions.checkNotNull( nodes );
		Preconditions.checkArgument( !nodes.isEmpty() );
		Preconditions.checkNotNull( procedure );
		Preconditions.checkArgument( timeout > 0 );
		final MultiProcedureCall<T, R> call = new MultiProcedureCall<>( nodes, procedure, argument, timeout );
		if ( !call.isDone() ) {
			openCalls.put( call );
			try {
				network.sendProcedureCall( call );
				if ( call.isDone() ) {
					openCalls.invalidate( call.getId() );
				}
			} catch ( Exception e ) {
				call.setException( e );
			}
		}
		return call;
	}
	
	public <T, R> void handle( final ProcedureCall<T, R> call ) {
		
		try {
			@SuppressWarnings( "unchecked" )
			CallableRegisteredProcedure<T, R> proc = ( CallableRegisteredProcedure<T, R> ) this.registeredProcedures.get( call.getProcedure() );
			if ( proc == null ) {
				throw new IllegalStateException( "no registered procedure" );
			}
			call.execute( proc );
		} catch ( Exception e ) {
			call.setException( e );
		}
		
	}
	
	public void handle( final ProcedureMessage msg ) throws ProtocolException {
		switch ( msg.getContentCase() ) {
			case CALL:
				if ( !this.getNetwork().getHome().isPart( msg.getTarget() ) ) {
					sendFail( msg, msg.getCall(), ErrorMessage.newBuilder().setType( ErrorMessage.Type.UNDEFINED ).setMessage( "wrong target" ) );
				}
				handle( msg, msg.getCall() );
				break;
			case RESPONSE:
				handle( msg, msg.getResponse() );
				break;
			default:
				throw new ProtocolException( "unknown procedure content!" );
		}
	}
	
	private void handle( final ProcedureMessage msg, final ProcedureResponseMessage response ) {
		ProcedureCall<?, ?> call = this.openCalls.get( response.getId() );
		if ( call != null ) {
			call.receive( msg, response );
		}
	}
	
	public static ProcedureResponseMessage.Builder newResponse( final ProcedureCallMessage call ) {
		ProcedureResponseMessage.Builder b = ProcedureResponseMessage.newBuilder();
		b.setProcedure( call.getProcedure() );
		b.setId( call.getId() );
		b.setTimestamp( call.getTimestamp() );
		return b;
	}
	
	private void sendFail( final ProcedureMessage msg, final ProcedureCallMessage call, final ErrorMessage.Builder error ) throws ProtocolException {
		ProcedureResponseMessage.Builder b = newResponse( call );
		b.setSuccess( false );
		b.setCancelled( false );
		b.setError( error );
		network.sendProcedureResponse( ProtocolUtils.convert( msg.getSender() ), b.build() );
	}
	
	private void handle( final ProcedureMessage msg, final ProcedureCallMessage call ) throws ProtocolException {
		
		final Procedure key = new Procedure( call.getProcedure() );
		final CallableRegisteredProcedure<?, ?> proc = this.registeredProcedures.get( key );
		if ( proc == null ) {
			sendFail( msg, call, ErrorMessage.newBuilder().setType( ErrorMessage.Type.UNDEFINED ).setMessage( "unregistered procedure" ) );
		}
		
		final Runnable run = () -> {
			try {
				
				ProcedureResponseMessage.Builder b = newResponse( call );
				proc.remoteCalled( call, b );
				b.setSuccess( true );
				b.setCancelled( false );
				network.sendProcedureResponse( ProtocolUtils.convert( msg.getSender() ), b.build() );
			} catch ( final Exception e ) {
				network.getLogger().log( Level.SEVERE, "Procedure handling failed\n" + e.getMessage(), e );
				try {
					sendFail( msg, call, ErrorMessage.newBuilder().setType( ErrorMessage.Type.UNDEFINED ).setMessage( "exception in procedure call + (" + e.getMessage() + ")" ) );
				} catch ( Exception e1 ) {
					network.getLogger().log( Level.SEVERE, e1.getMessage(), e1 );
				}
			}
		};
		
		if ( proc.isSynchronization() ) {
			this.getNetwork().syncExecute( run );
		} else {
			this.getNetwork().getExecutor().execute( run );
		}
		
	}
	
}
