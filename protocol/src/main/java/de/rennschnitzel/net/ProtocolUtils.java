package de.rennschnitzel.net;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import de.rennschnitzel.net.protocol.ComponentsProtocol.UUIDMessage;

public class ProtocolUtils {
	
	public static UUID convert( final UUIDMessage uuid ) {
		if ( uuid == null ) {
			return null;
		}
		return new UUID( uuid.getMostSignificantBits(), uuid.getLeastSignificantBits() );
	}
	
	public static UUIDMessage convert( final UUID uuid ) {
		if ( uuid == null ) {
			return null;
		}
		return UUIDMessage.newBuilder().setMostSignificantBits( uuid.getMostSignificantBits() ).setLeastSignificantBits( uuid.getLeastSignificantBits() ).build();
	}
	
	public static List<UUID> convertProto( final List<UUIDMessage> c ) {
		if ( c == null ) {
			return null;
		}
		final List<UUID> result = new ArrayList<>( c.size() );
		for ( int i = 0; i < c.size(); ++i ) {
			result.add( convert( c.get( i ) ) );
		}
		return result;
	}
	
	public static List<UUIDMessage> convert( final List<UUID> c ) {
		if ( c == null ) {
			return null;
		}
		final List<UUIDMessage> result = new ArrayList<>( c.size() );
		for ( int i = 0; i < c.size(); ++i ) {
			result.add( convert( c.get( i ) ) );
		}
		return result;
	}
	
	// private static final Function<UUID, ComponentUUID.UUID> toProtConv
	
	public static Collection<UUID> convertProto( Collection<UUIDMessage> c ) {
		if ( c == null ) {
			return null;
		}
		final List<UUID> result = new ArrayList<>( c.size() );
		c.forEach( old -> result.add( convert( old ) ) );
		return result;
	}
	
	public static Collection<UUIDMessage> convert( Collection<UUID> c ) {
		if ( c == null ) {
			return null;
		}
		final List<UUIDMessage> result = new ArrayList<>( c.size() );
		c.forEach( old -> result.add( convert( old ) ) );
		return result;
	}
	
}
