package de.mickare.net.exception;

import de.mickare.net.protocol.TransportProtocol.ErrorMessage.Type;

public class HandshakeException extends ConnectionException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5871338075781356108L;
	
	public HandshakeException() {
		super( Type.HANDSHAKE );
		// TODO Auto-generated constructor stub
	}
	
	public HandshakeException( String message ) {
		super( Type.HANDSHAKE, message );
		// TODO Auto-generated constructor stub
	}
	
	public HandshakeException( Throwable cause ) {
		super( Type.HANDSHAKE, cause );
		// TODO Auto-generated constructor stub
	}
	
	public HandshakeException( String message, Throwable cause ) {
		super( Type.HANDSHAKE, message, cause );
		// TODO Auto-generated constructor stub
	}
	
}
