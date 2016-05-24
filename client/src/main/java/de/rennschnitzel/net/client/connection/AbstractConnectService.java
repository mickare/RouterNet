package de.rennschnitzel.net.client.connection;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.rennschnitzel.net.NetClient;
import de.rennschnitzel.net.client.ConnectClient;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.NotConnectedException;
import de.rennschnitzel.net.netty.PipelineUtils;
import de.rennschnitzel.net.util.FutureUtils;
import de.rennschnitzel.net.util.concurrent.CloseableLock;
import de.rennschnitzel.net.util.concurrent.CloseableReadWriteLock;
import de.rennschnitzel.net.util.concurrent.ReentrantCloseableReadWriteLock;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Promise;
import lombok.AccessLevel;
import lombok.Getter;

public abstract class AbstractConnectService extends AbstractScheduledService implements ConnectService {
	
	private @Getter final NetClient client;
	private @Getter final long delay_time;
	private @Getter final TimeUnit delay_unit;
	
	private final CloseableReadWriteLock lock = new ReentrantCloseableReadWriteLock();
	private @Getter Promise<Connection> currentFuture = null;
	private ConnectClient connector = null;
	private @Getter( AccessLevel.PROTECTED ) EventLoopGroup group;
	
	private @Getter volatile int failCounter = 0;
	
	public AbstractConnectService( NetClient client ) {
		this( client, 1, TimeUnit.SECONDS );
	}
	
	public AbstractConnectService( NetClient client, long delay_time, TimeUnit delay_unit ) {
		Preconditions.checkNotNull( client );
		Preconditions.checkArgument( delay_time > 0 );
		Preconditions.checkArgument( delay_unit.toSeconds( delay_time ) > 0 );
		this.client = client;
		this.delay_time = delay_time;
		this.delay_unit = delay_unit;
	}
	
	@Override
	public Logger getLogger() {
		return client.getLogger();
	}
	
	@Override
	protected void startUp() throws Exception {
		getLogger().info( "Connect service: Event loop group created" );
		group = PipelineUtils.newEventLoopGroup( 0, new ThreadFactoryBuilder().setNameFormat( "Net-Netty IO Thread #%1$d" ).build() );
		getLogger().info( "Connect service: Started" );
		
		connectSoft();
	}
	
	@Override
	protected void shutDown() throws Exception {
		disconnect();
		group.shutdownGracefully().awaitUninterruptibly();
	}
	
	@Override
	protected void runOneIteration() throws Exception {
		ConnectClient con = connectSoft();
		if ( con != null ) {
			if ( this.isRunning() ) {
				con.awaitRunning( 1000 );
				if ( con.getState() != ConnectClient.State.ACTIVE ) {
					con.close();
				}
			} else {
				con.close();
			}
		}
	}
	
	private void disconnect() {
		try ( CloseableLock l = lock.writeLock().open() ) {
			if ( connector != null ) {
				connector.close();
			}
		}
	}
	
	private ConnectClient connectSoft() {
		if ( connector == null || connector.isClosed() ) {
			
			if ( currentFuture != null ) {
				
				if ( !currentFuture.isDone() ) {
					if ( connector.getState() == ConnectClient.State.FAILED ) {
						currentFuture.tryFailure( connector.getFailureCause() );
					} else {
						currentFuture.tryFailure( new NotConnectedException( "timed out" ) );
					}
				}
				
			}
			
			try ( CloseableLock l = lock.writeLock().open() ) {
				currentFuture = FutureUtils.newPromise();
				currentFuture.addListener( f -> {
					if ( !f.isSuccess() ) {
						if ( failCounter % 10 == 0 ) {
							if ( f.cause() != null ) {
								getLogger().log( Level.INFO, "Failed to connect: " + f.cause().getMessage() );
							} else {
								getLogger().log( Level.INFO, "Failed to connect: unknown cause" );
							}
						}
						failCounter++;
						handleFailConnect();
					} else {
						failCounter = 0;
					}
				} );
				connector = newConnectClient( currentFuture );
				if ( failCounter % 10 == 0 ) {
					if ( failCounter > 0 ) {
						getLogger().info( "Connecting to network... (Retry " + failCounter + ")" );
					} else {
						getLogger().info( "Connecting to network..." );
					}
				}
				connector.connect();
			}
		}
		return connector;
	}
	
	public void handleFailConnect() {
		this.client.handleFailConnect( this );
	}
	
	protected abstract ConnectClient newConnectClient( Promise<Connection> future );
	
	@Override
	protected Scheduler scheduler() {
		return Scheduler.newFixedDelaySchedule( delay_time, delay_time, delay_unit );
	}
	
}
