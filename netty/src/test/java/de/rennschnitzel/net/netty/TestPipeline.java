package de.rennschnitzel.net.netty;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.rennschnitzel.net.Owner;
import de.rennschnitzel.net.core.login.AuthenticationClient;
import de.rennschnitzel.net.core.login.AuthenticationFactory;
import de.rennschnitzel.net.core.login.AuthenticationRouter;
import de.rennschnitzel.net.core.login.LoginHandler;
import de.rennschnitzel.net.core.packet.BasePacketHandler;
import de.rennschnitzel.net.core.packet.PacketHandler;
import de.rennschnitzel.net.dummy.DummyNetwork;
import de.rennschnitzel.net.netty.login.NettyLoginRouterHandler;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginUpgradeMessage;
import de.rennschnitzel.net.util.SimpleOwner;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;

public class TestPipeline {

  private AuthenticationRouter authRouter = AuthenticationFactory.newPasswordForRouter("test");
  private AuthenticationClient authClient = AuthenticationFactory.newPasswordForClient("test");

  private Owner testingOwner;
  private DummyNetwork net_router;
  private DummyNetwork net_client;

  private Supplier<LoginHandler<ChannelHandlerContext>> router_loginHandler = //
      () -> new NettyLoginRouterHandler("routerLogin", net_router, authRouter) {
        @Override
        protected void upgradeConnection(ChannelHandlerContext ctx, LoginUpgradeMessage msg)
            throws Exception {}
      };

  private Supplier<PacketHandler<NettyConnection<DummyNetwork>>> router_packetHandler =
      () -> new BasePacketHandler<NettyConnection<DummyNetwork>>() {};

  private NioEventLoopGroup eventLoops;
  private final int serverPort = 10000;

  private ChannelFuture server;

  @Before
  public void setup() throws InterruptedException {
    testingOwner = new SimpleOwner("ChannelTestOwner", Logger.getLogger("ChannelTest"));

    net_router = new DummyNetwork();
    do {
      net_client = new DummyNetwork();
    } while (net_client.getHome().getId().equals(net_router.getHome().getId()));



    eventLoops = new NioEventLoopGroup(0,
        new ThreadFactoryBuilder().setNameFormat("Netty IO Thread #%1$d").build());
    ServerBootstrap b = new ServerBootstrap();
    b.group(eventLoops);
    b.option(ChannelOption.SO_REUSEADDR, true);
    b.childHandler(new BaseChannelInitializer(() -> new MainHandler<DummyNetwork>(net_router,
        router_loginHandler.get(), router_packetHandler.get())));
    ChannelFuture serverFuture = b.bind(serverPort);
    serverFuture.await(500, TimeUnit.MILLISECONDS);
  }

  @After
  public void tearDown() {
    eventLoops.shutdownGracefully();
    server.channel().close();
  }

  @Test
  public void testPipeline() {

    
    
  }

}
