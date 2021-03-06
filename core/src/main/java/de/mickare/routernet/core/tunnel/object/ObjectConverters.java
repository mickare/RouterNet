package de.mickare.routernet.core.tunnel.object;

import java.io.InvalidClassException;
import java.io.Serializable;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;

import de.mickare.routernet.core.Serialization;
import de.mickare.routernet.protocol.TransportProtocol;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public class ObjectConverters {
	
	private ObjectConverters() {
		// TODO Auto-generated constructor stub
	}
	
	public static TransportProtocol.TunnelRegister.Type getType( Class<?> dataClass ) throws InvalidClassException {
		if ( byte[].class.isAssignableFrom( dataClass ) ) {
			return TransportProtocol.TunnelRegister.Type.BYTES;
		}
		if ( Void.class.equals( dataClass ) ) {
			return TransportProtocol.TunnelRegister.Type.OBJECT;
		}
		if ( Serializable.class.isAssignableFrom( dataClass ) ) {
			return TransportProtocol.TunnelRegister.Type.OBJECT;
		}
		throw new InvalidClassException( dataClass.getName(), "data class not supported" );
	}
	
	@SuppressWarnings( "unchecked" )
	public static <T> ObjectConverter<T> of( Class<T> dataClass ) throws InvalidClassException {
		Preconditions.checkNotNull( dataClass );
		if ( byte[].class.isAssignableFrom( dataClass ) ) {
			return ( ObjectConverter<T> ) BYTE_ARRAY;
		}
		if ( Void.class.equals( dataClass ) ) {
			return ( ObjectConverter<T> ) VOID;
		}
		if ( Serializable.class.isAssignableFrom( dataClass ) ) {
			return ( ObjectConverter<T> ) SerializableObjectConverter.of( ( Class<? extends Serializable> ) dataClass );
		}
		throw new InvalidClassException( dataClass.getName(), "data class not supported" );
	}
	
	@RequiredArgsConstructor
	private static abstract class AbstractObjectConverter<T> implements ObjectConverter<T> {
		@NonNull
		protected final Class<T> dataClass;
		
		public boolean equals( Object o ) {
			if ( this == o ) {
				return true;
			}
			if ( !( o instanceof AbstractObjectConverter ) ) {
				return false;
			}
			return this.dataClass.equals( ( ( AbstractObjectConverter<?> ) o ).dataClass );
		}
	}
	
	private static class SerializableObjectConverter<T extends Serializable> extends AbstractObjectConverter<T> {
		
		public static <T extends Serializable> SerializableObjectConverter<T> of( Class<T> dataClass ) {
			return new SerializableObjectConverter<T>( dataClass );
		}
		
		public SerializableObjectConverter( Class<T> dataClass ) {
			super( dataClass );
		}
		
		@Override
		public final ByteString asByteString( final T obj ) throws ConvertObjectTunnelException {
			try {
				return Serialization.asByteString( this.dataClass, obj );
			} catch(Exception e) {
				throw new ConvertObjectTunnelException( e );
			}
		}
		
		@SuppressWarnings( "unchecked" )
		@Override
		public final T asObject( final ByteString byteData ) throws ConvertObjectTunnelException {			
			try {
				return ( T ) Serialization.asObject( this.dataClass, byteData );
			} catch(Exception e) {
				throw new ConvertObjectTunnelException( e );
			}
		}
	}
	
	public static final ObjectConverter<byte[]> BYTE_ARRAY = new AbstractObjectConverter<byte[]>( byte[].class ) {
		@Override
		public final ByteString asByteString( final byte[] obj ) throws ConvertObjectTunnelException {
			return ByteString.copyFrom( obj );
		}
		
		@Override
		public final byte[] asObject( final ByteString byteData ) throws ConvertObjectTunnelException {
			return byteData.toByteArray();
		}
	};
	
	public static final ObjectConverter<Void> VOID = new AbstractObjectConverter<Void>( Void.class ) {
		@Override
		public final ByteString asByteString( final Void obj ) throws ConvertObjectTunnelException {
			return ByteString.copyFrom( new byte[0] );
		}
		
		@Override
		public final Void asObject( final ByteString byteData ) throws ConvertObjectTunnelException {
			if ( byteData.size() > 0 ) {
				throw new ConvertObjectTunnelException( "unexpected byte data" );
			}
			return null;
		}
	};
	
}
