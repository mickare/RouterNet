package de.mickare.routernet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import de.mickare.routernet.protocol.ComponentsProtocol.UUIDMessage;

public class ProtocolUtils {
	
	/**
	 * Converts the UUID from protocol to java object
	 * @param uuid protocol message
	 * @return uuid as java object
	 */
	public static UUID convert( final UUIDMessage uuid ) {
		if ( uuid == null ) {
			return null;
		}
		return new UUID( uuid.getMostSignificantBits(), uuid.getLeastSignificantBits() );
	}
	/**
	 * Converts UUID from java object to protocol
	 * @param uuid java object
	 * @return uuid as protocol message
	 */
	public static UUIDMessage convert( final UUID uuid ) {
		if ( uuid == null ) {
			return null;
		}
		return UUIDMessage.newBuilder().setMostSignificantBits( uuid.getMostSignificantBits() ).setLeastSignificantBits( uuid.getLeastSignificantBits() ).build();
	}
	
	/**
	 * Converts a list of protocol UUIDs to java objects
	 * @param c list of uuid protocol messages
	 * @return  list of java uuid objects
	 */
	public static List<UUID> convertAllMessages( Collection<UUIDMessage> c ) {
		if ( c == null ) {
			return null;
		}
		final List<UUID> result = new ArrayList<>( c.size() );
		c.forEach( old -> result.add( convert( old ) ) );
		return result;
	}

	/**
	 * Converts a list of java UUID objects to protocol messages
	 * @param c list of java uuid objects
	 * @return  list of uuid protocol messages
	 */
	public static List<UUIDMessage> convertAll( Collection<UUID> c ) {
		if ( c == null ) {
			return null;
		}
		final List<UUIDMessage> result = new ArrayList<>( c.size() );
		c.forEach( old -> result.add( convert( old ) ) );
		return result;
	}
	
}
