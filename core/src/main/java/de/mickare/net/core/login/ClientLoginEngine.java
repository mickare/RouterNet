package de.mickare.net.core.login;

import java.util.Optional;
import java.util.UUID;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;

import de.mickare.net.Protocol;
import de.mickare.net.ProtocolUtils;
import de.mickare.net.core.AbstractNetwork;
import de.mickare.net.core.Connection;
import de.mickare.net.event.LoginSuccessEvent.RouterLoginSuccessEvent;
import de.mickare.net.exception.ProtocolException;
import de.mickare.net.protocol.LoginProtocol.LoginChallengeMessage;
import de.mickare.net.protocol.LoginProtocol.LoginHandshakeMessage;
import de.mickare.net.protocol.LoginProtocol.LoginResponseMessage;
import de.mickare.net.protocol.LoginProtocol.LoginSuccessMessage;
import de.mickare.net.protocol.LoginProtocol.LoginUpgradeMessage;
import lombok.Getter;

public class ClientLoginEngine extends LoginEngine {
	
	private final ClientAuthentication auth;
	private @Getter UUID loginId = null;
	private @Getter String loginName = null;
	private @Getter LoginSuccessMessage finalMessage = null;
	
	public ClientLoginEngine( AbstractNetwork network, ClientAuthentication authentication ) {
		super( network );
		Preconditions.checkNotNull( authentication );
		this.auth = authentication;
	}
	
	@Override
	public void start() throws Exception {
		Preconditions.checkState( this.getChannel() != null );
		checkAndSetState( State.NEW, State.LOGIN );
		LoginHandshakeMessage.Builder msg = LoginHandshakeMessage.newBuilder();
		msg.setProtocolVersion( Protocol.VERSION );
		msg.setId( ProtocolUtils.convert( getNetwork().getHome().getId() ) );
		Optional<String> name = getNetwork().getHome().getName();
		if ( name.isPresent() ) {
			msg.setName( name.get() );
		}
		this.getChannel().writeAndFlush( msg ).addListener( FAIL_LISTENER );
	}
	
	@Override
	public void handle( LoginEngine ctx, LoginHandshakeMessage msg ) throws Exception {
		throw new ProtocolException( "invalid packet" );
	}
	
	@Override
	public void handle( LoginEngine ctx, LoginResponseMessage msg ) throws Exception {
		throw new ProtocolException( "invalid packet" );
	}
	
	@Override
	public void handle( LoginEngine ctx, LoginChallengeMessage msg ) throws Exception {
		checkAndSetState( State.LOGIN, State.AUTH );
		ByteString response = this.auth.calculateResponse( msg.getToken() );
		this.getChannel().writeAndFlush( LoginResponseMessage.newBuilder().setToken( response ) ).addListener( FAIL_LISTENER );
	}
	
	@Override
	public void handle( LoginEngine ctx, LoginSuccessMessage msg ) throws Exception {
		checkState( State.AUTH );
		this.finalMessage = msg;
		this.loginId = ProtocolUtils.convert( msg.getRouterId() );
		this.loginName = msg.getRouterName();
		Preconditions.checkState( this.loginId != null );
		this.getChannel().writeAndFlush( LoginUpgradeMessage.newBuilder().setNode( this.getNetwork().getHome().toProtocol() ) ).addListener( FAIL_LISTENER ).addListener( f -> {
			if ( f.isSuccess() ) {
				setSuccess();
			}
		} );
	}
	
	@Override
	public void handle( LoginEngine ctx, LoginUpgradeMessage msg ) throws Exception {
		throw new ProtocolException( "invalid packet" );
	}
	
	@Override
	public RouterLoginSuccessEvent newLoginSuccessEvent( Connection connection ) {
		return new RouterLoginSuccessEvent( this.getNetwork(), this.getLoginId(), this.getLoginName(), connection, this.finalMessage.getTopology() );
	}
	
}
