package de.rennschnitzel.net.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import com.google.protobuf.ByteString;
import com.google.protobuf.ByteString.Output;

import de.rennschnitzel.net.core.tunnel.object.ConvertObjectTunnelException;

public class Serialization {
	
	public static FSTConfiguration FST = FSTConfiguration.createDefaultConfiguration();
	
	private Serialization() {
	}
	
	private static boolean USE_FST = false;
	
	public static byte[] asByteArray( Object obj ) {
		if ( USE_FST ) {
			return FST.asByteArray( obj );
		} else {
			try ( ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutput out = new ObjectOutputStream( bos ) ) {
				out.writeObject( obj );
				return bos.toByteArray();
			} catch ( IOException e ) {
				throw new RuntimeException( e );
			}
		}
	}
	
	public static <V> ByteString asByteString( Class<V> c, V obj ) throws IOException {
		if ( USE_FST ) {
			try ( final Output stream = ByteString.newOutput() ) {
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
			try ( final InputStream stream = byteData.newInput() ) {
				final FSTObjectInput in = FST.getObjectInput( stream );
				return in.readObject( c );
			}
		} else {
			try ( final InputStream stream = byteData.newInput(); ObjectInput in = new ObjectInputStream( stream ) ) {
				return in.readObject();
			}
		}
	}
	
	public static <V> Object asObject( Class<V> c, byte[] byteData ) throws Exception {
		if ( USE_FST ) {
			return FST.getObjectInput( byteData ).readObject( c );
		} else {
			try ( final InputStream stream = new ByteArrayInputStream( byteData ); ObjectInput in = new ObjectInputStream( stream ) ) {
				return in.readObject();
			}
		}
	}
}
