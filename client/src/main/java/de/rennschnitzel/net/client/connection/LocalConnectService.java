package de.rennschnitzel.net.client.connection;

import javax.net.ssl.SSLException;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.NetClient;
import de.rennschnitzel.net.client.TestFramework;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.login.ClientLoginEngine;
import de.rennschnitzel.net.core.login.RouterLoginEngine;
import de.rennschnitzel.net.core.packet.BasePacketHandler;
import de.rennschnitzel.net.netty.ConnectionHandler;
import de.rennschnitzel.net.netty.LocalConnectClient;
import de.rennschnitzel.net.netty.LoginHandler;
import de.rennschnitzel.net.netty.PipelineUtils;
import de.rennschnitzel.net.util.FutureUtils;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.local.LocalChannel;
import io.netty.util.concurrent.Promise;

public class LocalConnectService extends AbstractConnectService {
	
	private final TestFramework framework;
	
	public LocalConnectService( NetClient client, TestFramework framework ) throws SSLException {
		super( client );
		Preconditions.checkNotNull( framework );
		this.framework = framework;
	}
	
	@Override
	protected LocalConnectClient newConnectClient( Promise<Connection> promise ) {
		
		RouterLoginEngine engine_test = new RouterLoginEngine( framework.getRouterNetwork(), framework.getAuthenticationRouter() );
		ChannelInitializer<LocalChannel> init_test = PipelineUtils.baseInitAnd( ch -> {
			ch.pipeline().addLast( new LoginHandler( engine_test, FutureUtils.newPromise() ) );
			ch.pipeline().addLast( new ConnectionHandler( framework.getRouterNetwork(), BasePacketHandler.DEFAULT ) );
		} );
		
		ClientLoginEngine engine_client = new ClientLoginEngine( getClient().getNetwork(), getClient().getAuthentication() );
		ChannelInitializer<LocalChannel> init_client = PipelineUtils.baseInitAnd( ch -> {
			ch.pipeline().addLast( new LoginHandler( engine_client, promise ) );
			ch.pipeline().addLast( new ConnectionHandler( getClient().getNetwork(), BasePacketHandler.DEFAULT ) );
		} );
		
		return new LocalConnectClient( init_test, init_client, this.getGroup() );
	}
	
}
