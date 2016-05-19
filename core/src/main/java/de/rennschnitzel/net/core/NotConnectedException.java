package de.rennschnitzel.net.core;

public class NotConnectedException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6221878538268479901L;
	
	public NotConnectedException() {
	}
	
	public NotConnectedException( String message ) {
		super( message );
	}
	
	public NotConnectedException( Throwable cause ) {
		super( cause );
	}
	
	public NotConnectedException( String message, Throwable cause ) {
		super( message, cause );
	}
	
	public NotConnectedException( String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace ) {
		super( message, cause, enableSuppression, writableStackTrace );
	}
	
}
