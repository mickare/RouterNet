package de.mickare.net.core;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;

import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import com.google.protobuf.ByteString;
import com.google.protobuf.ByteString.Output;

import de.mickare.net.core.tunnel.object.ConvertObjectTunnelException;

public class Serialization {
	
	public static FSTConfiguration FST = FSTConfiguration.createDefaultConfiguration();
	
	private Serialization() {
	}
	
	private static boolean USE_FST = false;
	
	private static class ClassLoaderObjectInputStream extends ObjectInputStream {
		
		private final ClassLoader loader;
		
		public ClassLoaderObjectInputStream( InputStream in, Class<?> c ) throws IOException {
			this( in, c.getClassLoader() );
		}
		
		public ClassLoaderObjectInputStream( InputStream in, ClassLoader loader ) throws IOException {
			super( in );
			this.loader = loader;
		}
		
		protected Class<?> resolveClass( ObjectStreamClass desc ) throws ClassNotFoundException, IOException {
			try {
				return super.resolveClass( desc );
			} catch ( ClassNotFoundException e ) {
			
			}
			if ( loader == null ) {
				return Class.forName( desc.getName() );
			}
			return loader.loadClass( desc.getName() );
		}
	}
	
	public static <V> byte[] asByteArray( Class<V> c, V obj ) throws IOException {
		return asByteString( c, obj ).toByteArray();
	}
	
	public static <V> ByteString asByteString( Class<V> c, V obj ) throws IOException {
		if ( USE_FST ) {
			try ( final Output stream = ByteString.newOutput(); ) {
				final FSTObjectOutput out = FST.getObjectOutput( stream );
				out.writeObject( obj, c );
				out.flush();
				return stream.toByteString();
			} catch ( final IOException e ) {
				throw new ConvertObjectTunnelException( e );
			}
		} else {
			try ( final Output stream = ByteString.newOutput(); ObjectOutput out = new ObjectOutputStream( stream ) ) {
				out.writeObject( obj );
				out.flush();
				return stream.toByteString();
			} catch ( IOException e ) {
				throw new RuntimeException( e );
			}
		}
	}
	
	public static <V> Object asObject( Class<V> c, ByteString byteData ) throws Exception {
		if ( USE_FST ) {
			try ( final InputStream stream = byteData.newInput(); ) {
				final FSTObjectInput in = FST.getObjectInput( stream );
				return in.readObject( c );
			}
		} else {
			try ( final InputStream stream = byteData.newInput(); ObjectInput in = new ClassLoaderObjectInputStream( stream, c ) ) {
				return in.readObject();
			}
		}
	}
	
	public static <V> Object asObject( Class<V> c, byte[] byteData ) throws Exception {
		if ( USE_FST ) {
			return FST.getObjectInput( byteData ).readObject( c );
		} else {
			try ( final InputStream stream = new ByteArrayInputStream( byteData ); ObjectInput in = new ClassLoaderObjectInputStream( stream, c ) ) {
				return in.readObject();
			}
		}
	}
}
