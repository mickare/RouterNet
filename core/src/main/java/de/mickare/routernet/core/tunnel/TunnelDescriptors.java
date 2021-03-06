package de.mickare.routernet.core.tunnel;

import java.io.InvalidClassException;
import java.io.Serializable;

import de.mickare.routernet.core.tunnel.object.ObjectConverters;
import de.mickare.routernet.core.tunnel.object.ObjectTunnel;
import de.mickare.routernet.core.tunnel.stream.StreamTunnel;

public class TunnelDescriptors {
	
	private TunnelDescriptors() {
	}
	
	public static ObjectTunnel.Descriptor<byte[]> getByteTunnel( String name ) {
		return new ObjectTunnel.Descriptor<>( name, byte[].class, ObjectConverters.BYTE_ARRAY );
	}
	
	public static <T extends Serializable> ObjectTunnel.Descriptor<T> getObjectTunnel( String name, Class<T> dataClass ) {
		try {
			return new ObjectTunnel.Descriptor<>( name, dataClass );
		} catch ( InvalidClassException e ) {
			// should not happen
			throw new RuntimeException( e );
		}
	}
	
	public static StreamTunnel.Descriptor getStreamTunnel( String name ) {
		return new StreamTunnel.Descriptor( name );
	}
	
}
