package de.mickare.routernet.netty;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Preconditions;

import de.mickare.routernet.client.AbstractConnectClient;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

public @RequiredArgsConstructor class LocalConnectClient extends AbstractConnectClient {
	
	private static final AtomicInteger COUNTER = new AtomicInteger( 0 );
	
	private @Getter final LocalAddress addr = new LocalAddress( "localconnection." + COUNTER.getAndIncrement() );
	private @NonNull final ChannelInitializer<LocalChannel> serverInit;
	private @NonNull final ChannelInitializer<LocalChannel> clientInit;
	private @Getter @Setter boolean shutdownGroup = true;
	private EventLoopGroup group = null;
	private Channel serverChannel;
	private Channel clientChannel;
	
	public LocalConnectClient( ChannelInitializer<LocalChannel> serverInit, ChannelInitializer<LocalChannel> clientInit, EventLoopGroup group ) {
		this( serverInit, clientInit );
		Preconditions.checkArgument( !group.isShutdown() );
		this.group = group;
		this.shutdownGroup = false;
	}
	
	@Override
	protected void doConnect() throws Exception {
		if ( group == null ) {
			group = new DefaultEventLoopGroup();
		}
		
		Bootstrap cb = new Bootstrap();
		cb.remoteAddress( addr );
		cb.group( group ).channel( LocalChannel.class ).handler( clientInit );
		
		ServerBootstrap sb = new ServerBootstrap();
		sb.group( group ).channel( LocalServerChannel.class ).childHandler( serverInit );
		
		// Start server
		sb.bind( addr ).addListener( ( ChannelFutureListener ) sf -> {
			if ( sf.isSuccess() ) {
				serverChannel = sf.channel();
				cb.connect( addr ).addListener( ( ChannelFutureListener ) cf -> {
					if ( cf.isSuccess() ) {
						clientChannel = cf.channel();
						notfiyConnected();
					} else {
						notifyFailed( cf.cause() );
					}
				} );
			} else {
				notifyFailed( sf.cause() );
			}
		} );
	}
	
	@Override
	protected void doClose() throws Exception {
		if ( serverChannel != null ) {
			serverChannel.close().awaitUninterruptibly();
		}
		if ( clientChannel != null ) {
			clientChannel.close();
		}
		if ( shutdownGroup && group != null ) {
			group.shutdownGracefully();
		}
	}
	
}
