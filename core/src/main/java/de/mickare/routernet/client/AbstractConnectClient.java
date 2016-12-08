package de.mickare.routernet.client;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import lombok.Getter;

public abstract class AbstractConnectClient implements ConnectClient {
	
	private volatile @Getter State state = State.NEW;
	private @Getter Throwable failureCause = null;
	private final CountDownLatch latch = new CountDownLatch( 1 );
	
	public AbstractConnectClient connect() {
		Preconditions.checkState( state == State.NEW );
		state = State.STARTING;
		try {
			doConnect();
		} catch ( Exception e ) {
			notifyFailed( e );
		}
		return this;
	}
	
	protected abstract void doConnect() throws Exception;
	
	protected void notfiyConnected() {
		if ( this.state == State.STARTING ) {
			this.state = State.ACTIVE;
			latch.countDown();
		}
	}
	
	public void awaitRunning() throws InterruptedException {
		latch.await();
	}
	
	public boolean awaitRunning( long timeoutMillis ) throws InterruptedException {
		return latch.await( timeoutMillis, TimeUnit.MILLISECONDS );
	}
	
	protected void notifyFailed( Throwable cause ) {
		state = State.FAILED;
		failureCause = cause;
		_close();
	}
	
	@Override
	public void close() {
		if ( state == State.CLOSED || state == State.FAILED ) {
			return;
		}
		state = State.CLOSED;
		_close();
	}
	
	private void _close() {
		try {
			doClose();
		} catch ( Exception e ) {
			Logger.getGlobal().log( Level.SEVERE, "Exception while custom closing: " + e.getMessage(), e );
		}
		this.latch.countDown();
	}
	
	protected abstract void doClose() throws Exception;
	
	public boolean isClosed() {
		return this.state == State.CLOSED || this.state == State.FAILED;
	}
	
}
