package de.mickare.net.client;

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
	
	void awaitRunning() throws InterruptedException;
	
	boolean awaitRunning( long timeoutMillis ) throws InterruptedException;
	
	boolean isClosed();
	
	void close();
	
}
