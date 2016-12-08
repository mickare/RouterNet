package de.mickare.routernet.netty;

import java.security.cert.CertificateException;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;

import javax.net.ssl.SSLException;

import de.mickare.routernet.core.AbstractNetwork;
import de.mickare.routernet.protocol.TransportProtocol;
import de.mickare.routernet.util.ThrowableUtils;
import de.mickare.routernet.util.function.CheckedConsumer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.internal.PlatformDependent;

public class PipelineUtils {
	
	private PipelineUtils() {
	}
	
	private static boolean epoll;
	
	static {
		if ( !PlatformDependent.isWindows() && Boolean.parseBoolean( System.getProperty( "bungee.epoll", "false" ) ) ) {
			AbstractNetwork.getInstance().getLogger().info( "Not on Windows, attempting to use enhanced EpollEventLoop" );
			if ( epoll = Epoll.isAvailable() ) {
				AbstractNetwork.getInstance().getLogger().info( "Epoll is working, utilising it!" );
			} else {
				AbstractNetwork.getInstance().getLogger().log( Level.WARNING, "Epoll is not working, falling back to NIO: {0}", ThrowableUtils.exception( Epoll.unavailabilityCause() ) );
			}
		}
	}
	
	public static Class<? extends ServerChannel> getServerChannelClass() {
		return epoll ? EpollServerSocketChannel.class : NioServerSocketChannel.class;
	}
	
	public static Class<? extends Channel> getChannelClass() {
		return epoll ? EpollSocketChannel.class : NioSocketChannel.class;
	}
	
	public static EventLoopGroup newEventLoopGroup( int threads, ThreadFactory factory ) {
		return epoll ? new EpollEventLoopGroup( threads, factory ) : new NioEventLoopGroup( threads, factory );
	}
	
	public static SslContext sslContextForClient() throws SSLException {
		return SslContextBuilder.forClient().trustManager( InsecureTrustManagerFactory.INSTANCE ).build();
	}
	
	public static SslContext sslContextForServer() throws SSLException, CertificateException {
		SelfSignedCertificate ssc = new SelfSignedCertificate();
		return SslContextBuilder.forServer( ssc.certificate(), ssc.privateKey() ).build();
	}
	
	public static <C extends Channel> ChannelInitializer<C> baseInitAnd( final CheckedConsumer<C> init ) {
		return new ChannelInitializer<C>() {
			@Override
			protected void initChannel( C ch ) throws Exception {
				BASE_INITIALIZER.initChannel( ch );
				init.accept( ch );
			}
		};
	}
	
	public static final BaseInitializer BASE_INITIALIZER = new BaseInitializer();
	
	public static class BaseInitializer extends ChannelInitializer<Channel> {
		
		@Override
		public void initChannel( Channel ch ) throws Exception {
			ChannelPipeline p = ch.pipeline();
			
			p.addLast( "frameDecoder", new LengthFieldBasedFrameDecoder( 1048576, 0, 4, 0, 4 ) );
			p.addLast( "frameEncoder", new LengthFieldPrepender( 4 ) );
			// pipeline.addLast("compressionDecoder", new FastLzFrameDecoder(true));
			// pipeline.addLast("compressionEncoder", new FastLzFrameEncoder(true));
			p.addLast( "protoDecoder", new ProtobufDecoder( TransportProtocol.Packet.getDefaultInstance() ) );
			p.addLast( "protoEncoder", new ProtobufEncoder() );
		}
		
	}
	
}
