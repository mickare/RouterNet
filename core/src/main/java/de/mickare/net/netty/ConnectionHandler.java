package de.mickare.net.netty;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import de.mickare.net.core.AbstractNetwork;
import de.mickare.net.core.Connection;
import de.mickare.net.core.packet.PacketHandler;
import de.mickare.net.event.LoginSuccessEvent;
import de.mickare.net.event.LoginSuccessEvent.ClientLoginSuccessEvent;
import de.mickare.net.event.LoginSuccessEvent.RouterLoginSuccessEvent;
import de.mickare.net.exception.ConnectionException;
import de.mickare.net.protocol.TransportProtocol.CloseMessage;
import de.mickare.net.protocol.TransportProtocol.ErrorMessage;
import de.mickare.net.protocol.TransportProtocol.Packet;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.Getter;

public class ConnectionHandler extends SimpleChannelInboundHandler<Packet> {
	
	private @Getter final AbstractNetwork network;
	private final PacketHandler<Connection> handler;
	private @Getter Connection connection = null;
	
	public ConnectionHandler( AbstractNetwork network, PacketHandler<Connection> handler ) {
		super( Packet.class );
		Preconditions.checkNotNull( network );
		this.network = network;
		this.handler = handler;
	}
	
	@Override
	protected void channelRead0( ChannelHandlerContext ctx, Packet msg ) throws Exception {
		this.handler.handle( connection, msg );
	}
	
	@Override
	public void channelInactive( final ChannelHandlerContext ctx ) throws Exception {
		if ( connection != null ) {
			connection.removeFromNetwork();
		}
	}
	
	@Override
	public void exceptionCaught( ChannelHandlerContext ctx, Throwable cause ) throws Exception {
		try {
			ErrorMessage.Type type;
			String text;
			if ( cause instanceof ConnectionException ) {
				ConnectionException con = ( ConnectionException ) cause;
				type = con.getType();
				text = con.getMessage();
			} else {
				type = ErrorMessage.Type.SERVER_ERROR;
				text = cause.getMessage();
			}
			if ( text == null ) {
				text = "null";
			}
			ErrorMessage.Builder error = ErrorMessage.newBuilder().setType( type ).setMessage( text );
			
			ctx.writeAndFlush( CloseMessage.newBuilder().setError( error ) ).addListener( ChannelFutureListener.CLOSE );
		} finally {
			getLogger().log( Level.WARNING, "Channel Exception: " + cause.getMessage(), cause );
		}
	}
	
	public Logger getLogger() {
		return network.getLogger();
	}
	
	@Override
	public void userEventTriggered( ChannelHandlerContext ctx, Object evt ) throws Exception {
		
		if ( evt instanceof LoginSuccessEvent ) {
			
			LoginSuccessEvent login = ( LoginSuccessEvent ) evt;
			this.connection = login.getConnection();
			if ( login instanceof ClientLoginSuccessEvent ) {
				network.updateNode( this.connection, ( ( ClientLoginSuccessEvent ) login ).getNodeMessage() );
			} else if ( login instanceof RouterLoginSuccessEvent ) {
				network.updateNodes( this.connection, ( ( RouterLoginSuccessEvent ) login ).getNodeTopology() );
			}
			this.connection.addToNetwork();
			
		}
		
	}
	
}
