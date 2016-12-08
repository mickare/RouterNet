package de.mickare.routernet.client.connection;

import javax.net.ssl.SSLException;

import de.mickare.routernet.NetClient;
import de.mickare.routernet.client.OnlineConnectClient;
import de.mickare.routernet.core.Connection;
import de.mickare.routernet.core.login.ClientLoginEngine;
import de.mickare.routernet.core.packet.BasePacketHandler;
import de.mickare.routernet.netty.ConnectionHandler;
import de.mickare.routernet.netty.LoginHandler;
import de.mickare.routernet.netty.PipelineUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.Promise;

public class OnlineConnectService extends AbstractConnectService {
	
	private final SslContext sslCtx;
	
	public OnlineConnectService( NetClient client ) throws SSLException {
		super( client );
		this.sslCtx = PipelineUtils.sslContextForClient();
	}
	
	@Override
	protected OnlineConnectClient newConnectClient( Promise<Connection> future ) {
		
		ClientLoginEngine engine = new ClientLoginEngine( getClient().getNetwork(), getClient().getAuthentication() );
		
		ChannelInitializer<Channel> init = PipelineUtils.baseInitAnd( ch -> {
			ch.pipeline().addLast( new LoginHandler( engine, future ) );
			ch.pipeline().addLast( new ConnectionHandler( getClient().getNetwork(), BasePacketHandler.DEFAULT ) );
		} );
		
		return new OnlineConnectClient( getClient().getRouterAddress(), init, this.getGroup() );
	}
	
}
