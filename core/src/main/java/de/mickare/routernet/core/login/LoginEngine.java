package de.mickare.routernet.core.login;

import java.util.UUID;

import com.google.common.base.Preconditions;

import de.mickare.routernet.core.AbstractNetwork;
import de.mickare.routernet.core.Connection;
import de.mickare.routernet.core.packet.PacketHandler;
import de.mickare.routernet.event.LoginSuccessEvent;
import de.mickare.routernet.exception.ConnectionException;
import de.mickare.routernet.exception.HandshakeException;
import de.mickare.routernet.exception.ProtocolException;
import de.mickare.routernet.netty.ChannelWrapper;
import de.mickare.routernet.protocol.NetworkProtocol.NodeRemoveMessage;
import de.mickare.routernet.protocol.NetworkProtocol.NodeTopologyMessage;
import de.mickare.routernet.protocol.NetworkProtocol.NodeUpdateMessage;
import de.mickare.routernet.protocol.TransportProtocol.CloseMessage;
import de.mickare.routernet.protocol.TransportProtocol.ErrorMessage;
import de.mickare.routernet.protocol.TransportProtocol.HeartbeatMessage;
import de.mickare.routernet.protocol.TransportProtocol.Packet;
import de.mickare.routernet.protocol.TransportProtocol.ProcedureMessage;
import de.mickare.routernet.protocol.TransportProtocol.TunnelMessage;
import de.mickare.routernet.protocol.TransportProtocol.TunnelRegister;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public @Getter abstract class LoginEngine implements PacketHandler<LoginEngine> {
	
	public static @RequiredArgsConstructor enum State {
		NEW( 0 ),
		LOGIN( 1 ),
		AUTH( 1 ),
		SUCCESS( 3 ),
		FAILED( 3 );
		@Getter
		private final int step;
	}
	
	public final ChannelFutureListener FAIL_LISTENER = f -> {
		if ( !f.isSuccess() ) {
			this.fail( f.cause() );
		}
	};
	
	private volatile State state = State.NEW;
	private final AbstractNetwork network;
	private ChannelWrapper channel = null;
	private Promise<LoginEngine> promise;
	private State failureState = null;
	
	public LoginEngine( AbstractNetwork network ) {
		Preconditions.checkNotNull( network );
		this.network = network;
	}
	
	public void setChannel( ChannelWrapper channel ) {
		Preconditions.checkState( this.channel == null );
		Preconditions.checkNotNull( channel );
		this.channel = channel;
		this.promise = channel.getChannel().eventLoop().newPromise();
	}
	
	public Future<LoginEngine> getFuture() {
		return promise;
	}
	
	public Throwable getFailureCause() {
		return promise.cause();
	}
	
	public abstract void start() throws Exception;
	
	public abstract UUID getLoginId();
	
	public abstract String getLoginName();
	
	public abstract LoginSuccessEvent newLoginSuccessEvent( Connection connection );
	
	public boolean isSuccess() {
		return this.promise.isSuccess();
	}
	
	public boolean isDone() {
		return this.promise.isDone();
	}
	
	protected synchronized final void setState( final State state ) {
		Preconditions.checkArgument( state != State.FAILED );
		Preconditions.checkArgument( state != State.SUCCESS );
		Preconditions.checkArgument( state.step >= this.state.step, "Can only increase state step!" );
		this.state = state;
	}
	
	protected synchronized final void setSuccess() throws HandshakeException {
		if ( this.state.step >= State.SUCCESS.step ) {
			throw new HandshakeException( "Handshake already completed with " + this.state );
		}
		this.state = State.SUCCESS;
		this.promise.setSuccess( this );
	}
	
	public final synchronized boolean fail( Throwable cause ) {
		if ( !canFail() ) {
			return false;
		}
		this.failureState = this.state;
		this.state = State.FAILED;
		this.promise.setFailure( cause );
		return true;
	}
	
	private boolean canFail() {
		return this.state.step < State.FAILED.step;
	}
	
	public synchronized final void checkState( final State setpoint ) {
		if ( this.state != setpoint && canFail() ) {
			this.fail( new HandshakeException( "Wrong state (\"" + this.state.name() + "\" should be \"" + setpoint.name() + "\")!" ) );
		}
	}
	
	protected synchronized void checkAndSetState( final State expected, final State newState ) {
		this.checkState( expected );
		this.setState( newState );
	}
	
	// **************************************************************
	// Packet Handler
	
	@Override
	public void handle( LoginEngine ctx, Packet packet ) throws Exception {
		Preconditions.checkState( !this.isDone() );
		try {
			PacketHandler.super.handle( ctx, packet );
		} catch ( final Exception e ) {
			this.fail( e );
			throw e;
		}
	}
	
	@Override
	public void handle( LoginEngine ctx, CloseMessage msg ) throws Exception {
		switch ( msg.getReasonCase() ) {
			case ERROR:
				this.fail( ConnectionException.of( msg.getError() ) );
				break;
			case NORMAL:
				this.fail( new ConnectionException( ErrorMessage.Type.UNRECOGNIZED, msg.getNormal() ) );
				break;
			case SHUTDOWN:
				this.fail( new ConnectionException( ErrorMessage.Type.UNAVAILABLE, "shutdown" ) );
				break;
			default:
				this.fail( new ProtocolException( "invalid close reason" ) );
		}
		getChannel().close();
	}
	
	@Override
	public void handle( LoginEngine ctx, HeartbeatMessage heartbeat ) throws Exception {
		throw new HandshakeException( "Invalid or unknown packet!" );
	}
	
	@Override
	public void handle( LoginEngine ctx, NodeUpdateMessage msg ) throws HandshakeException {
		throw new HandshakeException( "Invalid or unknown packet!" );
	}
	
	@Override
	public void handle( LoginEngine ctx, NodeRemoveMessage msg ) throws HandshakeException {
		throw new HandshakeException( "Invalid or unknown packet!" );
	}
	
	@Override
	public void handle( LoginEngine ctx, NodeTopologyMessage msg ) throws HandshakeException {
		throw new HandshakeException( "Invalid or unknown packet!" );
	}
	
	@Override
	public void handle( LoginEngine ctx, TunnelMessage message ) throws HandshakeException {
		throw new HandshakeException( "Invalid or unknown packet!" );
	}
	
	@Override
	public void handle( LoginEngine ctx, ProcedureMessage msg ) throws HandshakeException {
		throw new HandshakeException( "Invalid or unknown packet!" );
	}
	
	@Override
	public void handle( LoginEngine ctx, TunnelRegister msg ) throws HandshakeException {
		throw new HandshakeException( "Invalid or unknown packet!" );
	}
	
}
