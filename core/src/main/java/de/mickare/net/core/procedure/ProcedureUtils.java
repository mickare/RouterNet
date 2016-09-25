package de.mickare.net.core.procedure;

import java.io.IOException;
import java.io.Serializable;
import java.util.function.BiConsumer;

import com.google.protobuf.ByteString;

import de.mickare.net.core.Serialization;
import de.mickare.net.protocol.TransportProtocol.ProcedureCallMessage;
import de.mickare.net.protocol.TransportProtocol.ProcedureResponseMessage;
import de.mickare.net.util.function.CheckedFunction;

public class ProcedureUtils {
	
	@SuppressWarnings( "unchecked" )
	public static <R> CheckedFunction<ProcedureResponseMessage, R> compileResponseReader( final Class<R> resultClass ) throws IllegalArgumentException {
		
		if ( Void.class.equals( resultClass ) ) {
			return ( p ) -> null;
		} else if ( byte[].class.equals( resultClass ) ) {
			return ( p ) -> ( R ) p.getBytes().toByteArray();
		} else if ( Serializable.class.isAssignableFrom( resultClass ) ) {
			return ( p ) -> ( R ) Serialization.asObject( resultClass, p.getObject() );
		} else {
			throw new IllegalArgumentException( "Unsupported response type (" + resultClass.getName() + ")!" );
		}
		
	}
	
	public static <R> BiConsumer<ProcedureResponseMessage.Builder, R> compileResponseWriter( Class<R> resultClass ) throws IllegalArgumentException {
		
		if ( Void.class.equals( resultClass ) ) {
			return ( p, r ) -> p.clearData();
		} else if ( byte[].class.equals( resultClass ) ) {
			return ( p, r ) -> p.setBytes( ByteString.copyFrom( ( byte[] ) r ) );
		} else if ( Serializable.class.isAssignableFrom( resultClass ) ) {
			return ( p, r ) -> {
				try {
					p.setObject( Serialization.asByteString( resultClass, r ) );
				} catch ( IOException e ) {
					throw new RuntimeException( e );
				}
			};
		} else {
			throw new IllegalArgumentException( "Unsupported response type (" + resultClass.getName() + ")!" );
		}
		
	}
	
	@SuppressWarnings( "unchecked" )
	public static <A> CheckedFunction<ProcedureCallMessage, A> compileCallReader( Class<A> argClass ) throws IllegalArgumentException {
		
		if ( Void.class.equals( argClass ) ) {
			return ( p ) -> null;
		} else if ( byte[].class.equals( argClass ) ) {
			return ( p ) -> ( A ) p.getBytes().toByteArray();
		} else if ( Serializable.class.isAssignableFrom( argClass ) ) {
			return ( p ) -> ( A ) Serialization.asObject( argClass, p.getObject() );
		} else {
			throw new IllegalArgumentException( "Unsupported call type (" + argClass.getName() + ")!" );
		}
		
	}
	
	public static <A> BiConsumer<ProcedureCallMessage.Builder, A> compileCallWriter( Class<A> argClass ) throws IllegalArgumentException {
		
		if ( Void.class.equals( argClass ) ) {
			return ( p, r ) -> p.clearData();
		} else if ( byte[].class.equals( argClass ) ) {
			return ( p, r ) -> p.setBytes( ByteString.copyFrom( ( byte[] ) r ) );
		} else if ( Serializable.class.isAssignableFrom( argClass ) ) {
			return ( p, r ) -> {
				try {
					p.setObject( Serialization.asByteString( argClass, r ) );
				} catch ( IOException e ) {
					throw new RuntimeException( e );
				}
			};
		} else {
			throw new IllegalArgumentException( "Unsupported call type (" + argClass.getName() + ")!" );
		}
		
	}
	
}
