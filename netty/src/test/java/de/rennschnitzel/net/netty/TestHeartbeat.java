package de.rennschnitzel.net.netty;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.net.HostAndPort;

import de.rennschnitzel.net.core.login.AuthenticationClient;
import de.rennschnitzel.net.core.login.AuthenticationFactory;
import de.rennschnitzel.net.core.login.AuthenticationRouter;
import de.rennschnitzel.net.core.login.LoginHandler;
import de.rennschnitzel.net.core.packet.BasePacketHandler;
import de.rennschnitzel.net.core.packet.PacketHandler;
import de.rennschnitzel.net.dummy.DummClientNetwork;
import de.rennschnitzel.net.dummy.DummyLogger;
import de.rennschnitzel.net.netty.login.NettyLoginClientHandler;
import de.rennschnitzel.net.netty.login.NettyLoginRouterHandler;
import de.rennschnitzel.net.protocol.TransportProtocol.HeartbeatMessage;
import de.rennschnitzel.net.util.concurrent.DirectScheduledExecutorService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;

public class TestHeartbeat {

  private Logger logger = new DummyLogger("TestHeartbeat", System.out);

  private AuthenticationRouter authRouter = AuthenticationFactory.newPasswordForRouter("test2");
  private AuthenticationClient authClient = AuthenticationFactory.newPasswordForClient("test2");

  private DummClientNetwork net_router;
  private DummClientNetwork net_client;

  private NettyServer server;
  private HostAndPort serverAddress = HostAndPort.fromParts("localhost", 10001);

  private NettyClient client;

  private final Promise<HeartbeatMessage> hearbeatPromise =
      ImmediateEventExecutor.INSTANCE.newPromise();
  private final AtomicLong heartbeatCounts = new AtomicLong(0);

  private void log(String msg) {
    logger.info(msg);
  }

  @Before
  public void setup() throws Exception {
    DirectScheduledExecutorService.disableWarning();
    log("setup start");

    long start = System.currentTimeMillis();



    // NETWORKS
    net_router = new DummClientNetwork();
    net_router.setName("Router");
    net_client = new DummClientNetwork(net_router.newNotUsedUUID());
    net_client.setName("Client");


    // ROUTER - HANDLERS
    Supplier<LoginHandler<ChannelHandlerContext>> router_loginHandler =
        () -> new NettyLoginRouterHandler(net_router, authRouter);
    Supplier<PacketHandler<NettyConnection<DummClientNetwork>>> router_packetHandler =
        () -> new BasePacketHandler<NettyConnection<DummClientNetwork>>() {
          @Override
          public void handle(NettyConnection<DummClientNetwork> ctx, HeartbeatMessage heartbeat)
              throws Exception {
            hearbeatPromise.trySuccess(heartbeat);
            heartbeatCounts.incrementAndGet();
          }
        };

    MainChannelInitializer serverInit =
        new MainChannelInitializer(() -> new MainHandler<DummClientNetwork>(net_router,
            router_loginHandler.get(), router_packetHandler.get()));


    // ROUTER - START
    server = new NettyServer("testServer", serverAddress, serverInit);
    server.startAsync();
    server.awaitRunning();


    // CLIENT - HANDLERS
    LoginHandler<ChannelHandlerContext> client_loginHandler =
        new NettyLoginClientHandler(net_client, authClient);
    PacketHandler<NettyConnection<DummClientNetwork>> client_packetHandler =
        new BasePacketHandler<NettyConnection<DummClientNetwork>>();

    MainChannelInitializer clientInit =
        new MainChannelInitializer(() -> new MainHandler<DummClientNetwork>(net_client, //
            client_loginHandler, client_packetHandler));


    // CLIENT - START
    client = new NettyClient("testClient", serverAddress, clientInit);
    client.connect().await(1, TimeUnit.SECONDS);
    if (!client_loginHandler.getConnectionPromise().await(3, TimeUnit.SECONDS)) {
      throw new TimeoutException();
    }

    log("setup complete (took " + (System.currentTimeMillis() - start) + "ms)");
  }


  @After
  public void tearDown() {
    log("tearDown start");
    long start = System.currentTimeMillis();


    server.stopAsync();
    if (server.hasFailed()) {
      logger.log(Level.WARNING, "server failed somewhere", server.failureCause());
    }


    client.disconnect();
    if (client.hasFailed()) {
      logger.log(Level.WARNING, "client failed somewhere", client.getFailureCause());
    }

    log("tearDown complete (took " + (System.currentTimeMillis() - start) + "ms)");
    DirectScheduledExecutorService.enableWarning();
  }


  @Test
  public void testHeartbeat()
      throws IOException, InterruptedException, TimeoutException, ExecutionException {
    log("testHeartbeat start");
    long start = System.currentTimeMillis();

    this.hearbeatPromise.await(10, TimeUnit.SECONDS);
    Thread.sleep(100);
    assertTrue(this.heartbeatCounts.get() > 0);

    log("testHeartbeat complete (took " + (System.currentTimeMillis() - start) + "ms)");
  }

}
