package de.mickare.routernet.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.function.Consumer;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.MoreExecutors;

import de.mickare.routernet.ProtocolUtils;
import de.mickare.routernet.core.Node.HomeNode;
import de.mickare.routernet.core.packet.Packer;
import de.mickare.routernet.core.procedure.ProcedureCall;
import de.mickare.routernet.core.tunnel.TunnelMessage;
import de.mickare.routernet.event.ConnectionAddedEvent;
import de.mickare.routernet.event.ConnectionRemovedEvent;
import de.mickare.routernet.exception.ProtocolException;
import de.mickare.routernet.protocol.TransportProtocol.Packet;
import de.mickare.routernet.protocol.TransportProtocol.ProcedureMessage;
import de.mickare.routernet.protocol.TransportProtocol.ProcedureResponseMessage;
import de.mickare.routernet.util.concurrent.CloseableLock;
import de.mickare.routernet.util.concurrent.CloseableReadWriteLock;
import de.mickare.routernet.util.concurrent.ReentrantCloseableReadWriteLock;
import de.mickare.routernet.util.function.Callback;

public abstract class AbstractClientNetwork extends AbstractNetwork {
	
	public AbstractClientNetwork( Logger logger, ScheduledExecutorService executor, HomeNode home ) {
		super( logger, executor, home );
	}
	
	private final CloseableReadWriteLock connectionLock = new ReentrantCloseableReadWriteLock();
	private final Condition connectedCondition = connectionLock.writeLock().newCondition();
	private Connection connection = null;
	private final Map<Callback<AbstractClientNetwork>, ExecutorService> connectCallbacks = Maps.newIdentityHashMap();
	
	// ***************************************************************************
	// Connection Getter
	
	/**
	 * Gets the connection object.
	 * 
	 * @deprecated unsafe to use
	 * @return Connection that can be in a unsafe state, or null
	 */
	@Deprecated
	public Connection getConnection() {
		try ( CloseableLock l = connectionLock.readLock().open() ) {
			return connection;
		}
	}
	
	/**
	 * Gets the connection object.
	 * 
	 * @deprecated unsafe to use
	 * @return Connection that can be in a unsafe state, or null
	 */
	@Deprecated
	public Connection getConnection( final UUID peerId ) {
		final Connection con = this.getConnection();
		return con != null ? ( peerId.equals( con.getPeerId() ) ? con : null ) : null;
	}
	
	/**
	 * Gets the connection object.
	 * 
	 * @deprecated unsafe to use
	 * @return Connection that can be in a unsafe state, or null
	 */
	@Deprecated
	public List<Connection> getConnections() {
		final Connection con = getConnection();
		return con != null ? Collections.singletonList( con ) : Collections.emptyList();
	}
	
	// ***************************************************************************
	// Connection State
	
	@Override
	public boolean isConnected() {
		try ( CloseableLock l = connectionLock.readLock().open() ) {
			return connection != null ? connection.isActive() : false;
		}
	}
	
	/**
	 * Await that the client is connected to the network.
	 * 
	 * @throws InterruptedException
	 */
	public void awaitConnected() throws InterruptedException {
		try ( CloseableLock l = connectionLock.writeLock().open() ) {
			if ( isConnected() ) {
				return;
			}
			connectedCondition.await();
		}
	}
	
	/**
	 * Causes the current thread to wait until it is signalled or interrupted, or the specified waiting time elapses.
	 * 
	 * @param time
	 *            the maximum time to wait
	 * @param unit
	 *            the time unit of the time argument
	 * @return false if the waiting time detectably elapsed before return from the method, else true
	 * @throws InterruptedException
	 *             - if the current thread is interrupted (and interruption of thread suspension is supported)
	 */
	public boolean awaitConnected( long time, TimeUnit unit ) throws InterruptedException {
		try ( CloseableLock l = connectionLock.writeLock().open() ) {
			if ( isConnected() ) {
				return true;
			}
			connectedCondition.await( time, unit );
			return isConnected();
		}
	}
	
	// ***************************************************************************
	// Callback
	
	private static final ExecutorService DIRECT_EXECUTOR = MoreExecutors.newDirectExecutorService();
	
	private void runConnectListener( final Callback<AbstractClientNetwork> callback, final ExecutorService executor ) {
		executor.execute( () -> {
			if ( AbstractClientNetwork.this.isConnected() ) {
				callback.call( AbstractClientNetwork.this );
			}
		} );
	}
	
	/**
	 * Add a callback that is directly executed when the server is connected.
	 * 
	 * @param callback
	 *            that is called
	 */
	public void addConnectCallback( final Callback<AbstractClientNetwork> callback ) {
		this.addConnectCallback( callback, DIRECT_EXECUTOR );
	}
	
	/**
	 * Adds a callback that is executed with the executor when the server is connected.
	 * 
	 * @param callback
	 *            that is called with executor
	 * @param executor
	 *            that is used to run the callback
	 */
	public void addConnectCallback( final Callback<AbstractClientNetwork> callback, final ExecutorService executor ) {
		Preconditions.checkNotNull( callback );
		Preconditions.checkNotNull( executor );
		try ( CloseableLock l = connectionLock.readLock().open() ) {
			
			if ( this.isConnected() ) {
				runConnectListener( callback, executor );
				return;
			}
		}
		
		try ( CloseableLock l = connectionLock.writeLock().open() ) {
			if ( this.isConnected() ) {
				runConnectListener( callback, executor );
			} else {
				connectCallbacks.put( callback, executor );
			}
		}
		
	}
	
	// ***************************************************************************
	// Connect
	
	@Override
	protected void addConnection( final Connection connection ) {
		Preconditions.checkNotNull( connection );
		try ( CloseableLock l = connectionLock.writeLock().open() ) {
			this.connection = connection;
			String name = connection.getName();
			getLogger().info( connection.getPeerId() + ( name != null ? "(" + name + ")" : "" ) + " connected." );
			
			this.getTunnels().forEach( t -> t.sendTunnelRegister( connection, false ) );
			connection.getChannel().flush();
			
			connectedCondition.signalAll();
		}
		runConnectCallbacks();
		this.getEventBus().post( new ConnectionAddedEvent( connection ) );
	}
	
	private void runConnectCallbacks() {
		try ( CloseableLock l = connectionLock.writeLock().open() ) {
			for ( Entry<Callback<AbstractClientNetwork>, ExecutorService> e : this.connectCallbacks.entrySet() ) {
				runConnectListener( e.getKey(), e.getValue() );
			}
			this.connectCallbacks.clear();
		}
	}
	
	protected void removeConnection( final Connection connection ) {
		Preconditions.checkNotNull( connection );
		try ( CloseableLock l = connectionLock.writeLock().open() ) {
			if ( this.connection == connection ) {
				this.connection = null;
			}
			if ( connection.isActive() ) {
				connection.getChannel().close();
			}
			String name = connection.getName();
			getLogger().info( connection.getPeerId() + ( name != null ? "(" + name + ")" : "" ) + " disconnected." );
		}
		this.getEventBus().post( new ConnectionRemovedEvent( connection ) );
	}
	
	@Override
	public <T, R> void sendProcedureCall( ProcedureCall<T, R> call ) {
		
		if ( !call.getTarget().isOnly( this.getHome() ) ) {
			ProcedureMessage.Builder b = ProcedureMessage.newBuilder();
			b.setTarget( call.getTarget().getProtocolMessage() );
			b.setSender( ProtocolUtils.convert( getHome().getId() ) );
			b.setCall( call.toProtocol() );
			if ( !send( Packer.pack( b.build() ) ) ) {
				call.setException( new NotConnectedException() );
			}
		}
		if ( call.getTarget().contains( this.getHome() ) ) {
			this.getProcedureManager().handle( call );
		}
	}
	
	@Override
	protected void sendProcedureResponse( final UUID senderId, final UUID receiverId, final ProcedureResponseMessage msg ) throws ProtocolException {
		final ProcedureMessage pmsg = ProcedureMessage.newBuilder().setSender( ProtocolUtils.convert( senderId ) ).setTarget( Target.to( receiverId ).getProtocolMessage() ).setResponse( msg ).build();
		if ( this.getHome().getId().equals( receiverId ) ) {
			this.getProcedureManager().handle( pmsg );
		} else {
			send( Packer.pack( pmsg ) );
		}
	}
	
	private boolean send( final Packet packet ) {
		try ( CloseableLock l = connectionLock.readLock().open() ) {
			if ( connection != null && connection.isActive() ) {
				connection.writeAndFlushFast( packet );
				return true;
			}
		}
		return false;
	}
	
	private boolean send( final Consumer<Connection> out ) {
		try ( CloseableLock l = connectionLock.readLock().open() ) {
			if ( connection != null ) {
				out.accept( connection );
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean publishHomeNodeUpdate() {
		return send( getHome()::sendUpdate );
	}
	
	@Override
	protected boolean sendTunnelMessage( final TunnelMessage cmsg ) {
		return send( Packer.pack( cmsg.toProtocolMessage() ) );
	}
	
	@Override
	protected boolean registerTunnel( final Tunnel tunnel ) {
		return send( tunnel::sendTunnelRegister );
	}
	
}
