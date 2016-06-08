package de.rennschnitzel.net.core.procedure;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.protocol.NetworkProtocol.ProcedureDescription;
import de.rennschnitzel.net.util.LazyCache;
import de.rennschnitzel.net.util.TypeUtils;
import lombok.Getter;

public class Procedure implements Comparable<Procedure> {
	
	// ***************************************************************************
	// STATIC - START
	
	private static final Object LOCK = new Object();
	
	public static <T, R> BoundProcedure<T, R> of( String name, Function<T, R> function ) {
		return of( name, function, AbstractNetwork.getInstance() );
	}
	
	public static <T, R> BoundProcedure<T, R> of( String name, Function<T, R> function, AbstractNetwork network ) {
		return BoundProcedure.of( name, function, network );
	}
	
	public static <T> BoundProcedure<T, Void> of( String name, Consumer<T> consumer ) {
		return of( name, consumer, AbstractNetwork.getInstance() );
	}
	
	public static <T> BoundProcedure<T, Void> of( String name, Consumer<T> consumer, AbstractNetwork network ) {
		return BoundProcedure.of( name, consumer, network );
	}
	
	public static <R> BoundProcedure<Void, R> of( String name, Supplier<R> supplier ) {
		return of( name, supplier, AbstractNetwork.getInstance() );
	}
	
	public static <R> BoundProcedure<Void, R> of( String name, Supplier<R> supplier, AbstractNetwork network ) {
		return BoundProcedure.of( name, supplier, network );
	}
	
	public static BoundProcedure<Void, Void> of( String name, Runnable runnable ) {
		return of( name, runnable, AbstractNetwork.getInstance() );
	}
	
	public static BoundProcedure<Void, Void> of( String name, Runnable runnable, AbstractNetwork network ) {
		return BoundProcedure.of( name, runnable, network );
	}
	
	public static <T, R> CallableProcedure<T, R> of( String name, Class<T> argument, Class<R> result ) {
		return of( name, argument, result, AbstractNetwork.getInstance() );
	}
	
	public static <T, R> CallableProcedure<T, R> of( String name, Class<T> argument, Class<R> result, AbstractNetwork network ) {
		return new CallableProcedure<T, R>( network, name, argument, result );
	}
	
	public static Procedure of( String name, String argumentType, String resultType ) {
		return new Procedure( name, argumentType, resultType );
	}
	
	// STATIC - END
	// ***************************************************************************
	
	private @Getter final String name, argumentType, resultType;
	private transient final LazyCache<Class<?>> argumentClass;
	private transient final LazyCache<Class<?>> resultClass;
	
	public Procedure( final ProcedureDescription msg ) throws IllegalArgumentException, NullPointerException {
		this( msg.getName(), msg.getArgumentType(), msg.getResultType() );
	}
	
	/*
	 * public ProcedureInformation(String name, Function<?, ?> function) { this(name,
	 * TypeUtils.resolveArgumentClass(function)); }
	 * 
	 * private ProcedureInformation(String name, Class<?>[] typeArgs) { this(name, typeArgs[0], typeArgs[1]); }
	 */
	
	public Procedure( final String name, final Class<?> argument, final Class<?> result ) {
		this( name, argument.getName(), result.getName() );
		this.argumentClass.set( argument );
		this.resultClass.set( result );
	}
	
	public Procedure( final String name, final String argumentType, final String resultType ) throws IllegalArgumentException, NullPointerException {
		Preconditions.checkArgument( !name.isEmpty() );
		Preconditions.checkArgument( !argumentType.isEmpty() );
		Preconditions.checkArgument( !resultType.isEmpty() );
		this.name = name.toLowerCase();
		this.argumentType = argumentType;
		this.resultType = resultType;
		
		argumentClass = LazyCache.of( () -> {
			synchronized ( LOCK ) {
				return Class.forName( this.argumentType );
			}
		} );
		
		resultClass = LazyCache.of( () -> {
			synchronized ( LOCK ) {
				return Class.forName( this.resultType );
			}
		} );
	}
	
	public <T, R> CallableProcedure<T, R> bind( Function<T, R> function ) throws IllegalArgumentException {
		return bind( AbstractNetwork.getInstance(), function );
	}
	
	public <T> CallableProcedure<T, Void> bind( Consumer<T> consumer ) throws IllegalArgumentException {
		return bind( AbstractNetwork.getInstance(), consumer );
	}
	
	public <R> CallableProcedure<Void, R> bind( Supplier<R> supplier ) throws IllegalArgumentException {
		return bind( AbstractNetwork.getInstance(), supplier );
	}
	
	public CallableProcedure<Void, Void> bind( Runnable runnable ) throws IllegalArgumentException {
		return bind( AbstractNetwork.getInstance(), runnable );
	}
	
	public <T, R> CallableProcedure<T, R> bind( final Class<T> argument, final Class<R> result ) throws IllegalArgumentException {
		return bind( AbstractNetwork.getInstance(), argument, result );
	}
	
	@SuppressWarnings( "unchecked" )
	public <T, R> CallableProcedure<T, R> bind( AbstractNetwork network, Function<T, R> function ) throws IllegalArgumentException {
		Class<?>[] typeArgs = TypeUtils.resolveArgumentClass( function );
		return bind( network, ( Class<T> ) typeArgs[0], ( Class<R> ) typeArgs[1] );
	}
	
	public <T> CallableProcedure<T, Void> bind( AbstractNetwork network, Consumer<T> consumer ) throws IllegalArgumentException {
		return bind( network, ( Class<T> ) TypeUtils.resolveArgumentClass( consumer ), Void.class );
	}
	
	public <R> CallableProcedure<Void, R> bind( AbstractNetwork network, Supplier<R> supplier ) throws IllegalArgumentException {
		return bind( network, Void.class, ( Class<R> ) TypeUtils.resolveArgumentClass( supplier ) );
	}
	
	public CallableProcedure<Void, Void> bind( AbstractNetwork network, Runnable runnable ) throws IllegalArgumentException {
		return bind( network, Void.class, Void.class );
	}
	
	public <T, R> CallableProcedure<T, R> bind( AbstractNetwork network, final Class<T> argumentType, final Class<R> resultType ) throws IllegalArgumentException {
		checkApplicable( argumentType, resultType );
		return new CallableProcedure<>( network, this, argumentType, resultType );
	}
	
	public Procedure checkApplicable( final Class<?> argumentType, final Class<?> resultType ) throws IllegalArgumentException {
		Preconditions.checkArgument( getArgumentClass().isAssignableFrom( argumentType ) );
		Preconditions.checkArgument( getResultClass().isAssignableFrom( resultType ) );
		return this;
	}
	
	public boolean isApplicable( final Class<?> argumentType, final Class<?> resultType ) throws RuntimeException {
		boolean result = true;
		result &= getArgumentClass().isAssignableFrom( argumentType );
		result &= getResultClass().isAssignableFrom( resultType );
		return result;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash( name, argumentType, resultType );
	}
	
	public Class<?> getArgumentClass() throws RuntimeException {
		try {
			return this.argumentClass.get();
		} catch ( Exception e ) {
			throw new RuntimeException( e );
		}
	}
	
	public Class<?> getResultClass() throws RuntimeException {
		try {
			return this.resultClass.get();
		} catch ( Exception e ) {
			throw new RuntimeException( e );
		}
	}
	
	@Override
	public boolean equals( final Object o ) {
		if ( o == this )
			return true;
		if ( !( o instanceof Procedure ) )
			return false;
		final Procedure oi = ( Procedure ) o;
		//if ( !name.equals( oi.name ) || !argumentType.equals( oi.argumentType ) || !resultType.equals( oi.resultType ) )
		//	return false;
		return compareTo( oi ) == 0;
	}
	
	@Override
	public int compareTo( Procedure o ) {
		if ( this == o ) {
			return 0;
		}
		int n = String.CASE_INSENSITIVE_ORDER.compare( name, o.name );
		if ( n != 0 ) {
			return n;
		}
		int arg = String.CASE_INSENSITIVE_ORDER.compare( argumentType, o.argumentType );
		if ( arg != 0 ) {
			return arg;
		}
		return String.CASE_INSENSITIVE_ORDER.compare( resultType, o.resultType );
	}
	
	public ProcedureDescription toProtocol() {
		ProcedureDescription.Builder b = ProcedureDescription.newBuilder();
		b.setName( this.name );
		b.setArgumentType( this.argumentType );
		b.setResultType( this.resultType );
		return b.build();
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[" + this.resultType + " " + this.name + "(" + this.argumentType + ") ]";
	}
	
}
