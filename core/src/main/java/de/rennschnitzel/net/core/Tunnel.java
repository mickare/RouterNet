package de.rennschnitzel.net.core;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.logging.Level;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;

import de.rennschnitzel.net.Owner;
import de.rennschnitzel.net.core.Node.HomeNode;
import de.rennschnitzel.net.core.tunnel.TunnelHandler;
import de.rennschnitzel.net.core.tunnel.TunnelMessage;
import de.rennschnitzel.net.protocol.TransportProtocol;
import de.rennschnitzel.net.protocol.TransportProtocol.TunnelRegister;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public class Tunnel {
	
	private transient @Getter final AbstractNetwork network;
	private @Getter final String name;
	private @Getter final int id;
	private @Getter volatile boolean closed = false;
	private transient @Getter Optional<Executor> executor = Optional.empty();
	
	private Optional<TunnelRegister.Type> type = Optional.empty();
	private transient TunnelHandler handler = null;
	private transient final CopyOnWriteArraySet<RegisteredMessageListener> listeners = new CopyOnWriteArraySet<>();
	
	public Tunnel( final AbstractNetwork network, final String name ) {
		Preconditions.checkNotNull( network );
		Preconditions.checkArgument( !name.isEmpty() );
		this.network = network;
		this.name = name.toLowerCase();
		this.id = this.name.hashCode();
	}
	
	public void setExectutor( Executor executor ) {
		this.executor = Optional.ofNullable( executor );
	}
	
	public synchronized void registerHandler( final TunnelHandler handler ) throws IllegalStateException {
		Preconditions.checkNotNull( handler );
		Preconditions.checkState( this.handler == null );
		if ( this.type.isPresent() ) {
			Preconditions.checkState( handler.getType() == this.type.get() );
		}
		this.handler = handler;
		this.type = Optional.of( handler.getType() );
	}
	
	public synchronized void setType( final TunnelRegister.Type type ) {
		Preconditions.checkNotNull( type );
		if ( this.type.isPresent() ) {
			if ( this.type.get() == type ) {
				return;
			}
			throw new IllegalStateException( "Type already defined as " + this.type.get() + "!" );
		}
		this.type = Optional.of( type );
	}
	
	public TunnelRegister.Type getType() {
		return type.orElse( TunnelRegister.Type.BYTES );
	}
	
	public void close() {
		this.closed = true;
	}
	
	public boolean broadcast( final ByteBuffer data ) {
		return this.send( Target.toAll(), data );
	}
	
	public boolean broadcast( final byte[] data ) {
		return this.send( Target.toAll(), data );
	}
	
	public boolean broadcast( final ByteString data ) {
		return this.send( Target.toAll(), data );
	}
	
	public boolean send( final Target target, final ByteBuffer data ) {
		return this.send( target, ByteString.copyFrom( data ) );
	}
	
	public boolean send( final Target target, final byte[] data ) {
		return this.send( target, ByteString.copyFrom( data ) );
	}
	
	public boolean send( final Target target, final ByteString data ) {
		final TunnelMessage cmsg = new TunnelMessage( this, target, getNetwork().getHome().getId(), data );
		return this.send( cmsg );
	}
	
	public boolean send( final TunnelMessage cmsg ) {
		final HomeNode home = getNetwork().getHome();
		boolean success = _sendIgnoreSelf( home, cmsg );
		if ( cmsg.getTarget().contains( home ) ) {
			this.receive( null, cmsg );
		}
		return success;
	}

	public boolean sendIgnoreSelf( final TunnelMessage cmsg ) {
		return _sendIgnoreSelf( getNetwork().getHome(), cmsg );
	}
	
	private boolean _sendIgnoreSelf( HomeNode home, final TunnelMessage cmsg ) {
		if ( !cmsg.getTarget().isOnly( home ) ) {
			return this.network.sendTunnelMessage( cmsg );
		}
		return true;
	}
	
	public final void receiveProto( final Connection con, final TransportProtocol.TunnelMessage msg ) {
		if ( this.listeners.size() > 0 || this.handler != null ) {
			// Only handle it if there are handlers.
			this.receive( con, new TunnelMessage( this, msg ) );
		}
	}
	
	public final void receive( final Connection con, final TunnelMessage cmsg ) {
		if ( this.listeners.size() > 0 ) {
			if ( con != null ) {
				this.executor.orElseGet( con.getChannel().getChannel()::eventLoop ).execute( () -> {
					this.listeners.forEach( c -> c.accept( cmsg ) );
				} );
			} else {
				this.listeners.forEach( c -> c.accept( cmsg ) );
			}
		}
		if ( this.handler != null ) {
			try {
				this.handler.receive( con, cmsg );
			} catch ( Exception e ) {
				this.getNetwork().getLogger().log( Level.SEVERE, "Channel handler exception: " + e.getMessage(), e );
			}
		}
	}
	
	public final void registerListener( final Owner owner, final Consumer<TunnelMessage> dataConsumer ) {
		listeners.add( new RegisteredMessageListener( owner, dataConsumer ) );
	}
	
	public final void unregisterListeners( final Owner owner ) {
		this.listeners.removeIf( ( l ) -> l.getOwner().equals( owner ) );
	}
	
	public void sendTunnelRegister( Connection connection ) {
		sendTunnelRegister( connection, true );
	}
	
	public void sendTunnelRegister( Connection connection, boolean flush ) {
		TunnelRegister.Builder b = TunnelRegister.newBuilder();
		b.setTunnelId( this.getId() );
		b.setName( this.getName() );
		b.setType( this.getType() );
		if ( flush ) {
			connection.writeAndFlushFast( b.build() );
		} else {
			connection.writeFast( b.build() );
		}
	}
	
	private @Getter @RequiredArgsConstructor class RegisteredMessageListener implements Consumer<TunnelMessage> {
		
		private @NonNull final Owner owner;
		private @NonNull final Consumer<TunnelMessage> delegate;
		
		@Override
		public void accept( TunnelMessage cmsg ) {
			try {
				delegate.accept( cmsg );
			} catch ( Exception e ) {
				getNetwork().getLogger().log( Level.SEVERE, "Message listener of " + owner + " threw exception: " + e.getMessage(), e );
			}
		}
	}
	
}
