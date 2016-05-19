package de.rennschnitzel.net.service;

public interface ConnectClient extends AutoCloseable {
	
	public static enum State {
		NEW,
		STARTING,
		ACTIVE,
		CLOSED,
		FAILED;
	};
	
	State getState();
	
	Throwable getFailureCause();
	
	ConnectClient connect();
	
	ConnectClient awaitRunning() throws InterruptedException;
	
	ConnectClient awaitRunning( long timeoutMillis ) throws InterruptedException;
	
	boolean isClosed();
	
	void close();
	
}
