package de.mickare.routernet.exception;

import java.io.IOException;

import com.google.common.base.Preconditions;

import de.mickare.routernet.protocol.TransportProtocol.ErrorMessage;
import lombok.Getter;

@SuppressWarnings( "serial" )
public class ConnectionException extends IOException {
	
	@Getter
	private final ErrorMessage.Type type;
	
	public static ConnectionException of( ErrorMessage error ) {
		if ( error.getType() == ErrorMessage.Type.HANDSHAKE ) {
			return new HandshakeException( error.getMessage() );
		} else if ( error.getType() == ErrorMessage.Type.PROTOCOL_ERROR ) {
			return new ProtocolException( error.getMessage() );
		}
		return new ConnectionException( error.getType(), error.getMessage() );
	}
	
	public ConnectionException( ErrorMessage.Type type ) {
		Preconditions.checkNotNull( type );
		this.type = type;
	}
	
	public ConnectionException( ErrorMessage.Type type, String message ) {
		super( message );
		Preconditions.checkNotNull( type );
		this.type = type;
	}
	
	public ConnectionException( ErrorMessage.Type type, Throwable cause ) {
		super( cause );
		Preconditions.checkNotNull( type );
		this.type = type;
	}
	
	public ConnectionException( ErrorMessage.Type type, String message, Throwable cause ) {
		super( message, cause );
		Preconditions.checkNotNull( type );
		this.type = type;
	}
	
}
