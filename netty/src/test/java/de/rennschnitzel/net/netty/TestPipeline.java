package de.rennschnitzel.net.netty;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.net.HostAndPort;

import de.rennschnitzel.net.Owner;
import de.rennschnitzel.net.core.login.AuthenticationClient;
import de.rennschnitzel.net.core.login.AuthenticationFactory;
import de.rennschnitzel.net.core.login.AuthenticationRouter;
import de.rennschnitzel.net.core.login.LoginHandler;
import de.rennschnitzel.net.core.packet.BasePacketHandler;
import de.rennschnitzel.net.core.packet.PacketHandler;
import de.rennschnitzel.net.core.procedure.ProcedureInformation;
import de.rennschnitzel.net.dummy.DummyLogger;
import de.rennschnitzel.net.dummy.DummyNetwork;
import de.rennschnitzel.net.netty.login.NettyLoginClientHandler;
import de.rennschnitzel.net.netty.login.NettyLoginRouterHandler;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import de.rennschnitzel.net.util.SimpleOwner;
import io.netty.channel.ChannelHandlerContext;

public class TestPipeline {

  private Logger logger = new DummyLogger("TestPipeline", System.out);

  private AuthenticationRouter authRouter = AuthenticationFactory.newPasswordForRouter("test");
  private AuthenticationClient authClient = AuthenticationFactory.newPasswordForClient("test");

  private Owner testingOwner;
  private DummyNetwork net_router;
  private DummyNetwork net_client;

  private NettyServer server;
  private HostAndPort serverAddress = HostAndPort.fromParts("localhost", 10000);

  private NettyClient client;

  private void log(String msg) {
    logger.info(msg);
  }

  @Before
  public void setup() throws Exception {
    log("setup start");

    long start = System.currentTimeMillis();

    testingOwner = new SimpleOwner("ChannelTestOwner", Logger.getLogger("ChannelTest"));

    // NETWORKS
    net_router = new DummyNetwork();
    net_router.setName("Router");
    do {
      net_client = new DummyNetwork();
      net_client.setName("Client");
    } while (net_client.getHome().getId().equals(net_router.getHome().getId()));

    // ROUTER - HANDLERS
    Supplier<LoginHandler<ChannelHandlerContext>> router_loginHandler =
        () -> new NettyLoginRouterHandler(net_router, authRouter);
    Supplier<PacketHandler<NettyConnection<DummyNetwork>>> router_packetHandler =
        () -> new BasePacketHandler<NettyConnection<DummyNetwork>>();

    BaseChannelInitializer serverInit =
        new BaseChannelInitializer(() -> new MainHandler<DummyNetwork>(net_router,
            router_loginHandler.get(), router_packetHandler.get()));


    // ROUTER - START
    server = new NettyServer("testServer", serverAddress, serverInit);
    server.startAsync();
    server.awaitRunning();


    // CLIENT - HANDLERS
    LoginHandler<ChannelHandlerContext> client_loginHandler =
        new NettyLoginClientHandler(net_client, authClient);
    Supplier<PacketHandler<NettyConnection<DummyNetwork>>> client_packetHandler =
        () -> new BasePacketHandler<NettyConnection<DummyNetwork>>();

    BaseChannelInitializer clientInit =
        new BaseChannelInitializer(() -> new MainHandler<DummyNetwork>(net_client,
            client_loginHandler, client_packetHandler.get()));


    // CLIENT - START
    client = new NettyClient("testClient", serverAddress, clientInit);
    client.connect().get(1, TimeUnit.SECONDS);
    client_loginHandler.getConnectionFuture().get(3, TimeUnit.SECONDS);

    log("setup complete (took " + (System.currentTimeMillis() - start) + "ms)");
  }

  @After
  public void tearDown() {
    log("tearDown start");
    long start = System.currentTimeMillis();

    server.stopAsync();
    client.disconnect();

    log("tearDown complete (took " + (System.currentTimeMillis() - start) + "ms)");
  }

  @Test
  public void testNodes() throws IOException {


    net_client.getConnection()
        .send(Packet.newBuilder().setClose(CloseMessage.newBuilder().setNormal("Test")));

    log(net_router.getNodes().toString());

    // Test Node
    assertNotNull(net_router.getNode(net_client.getHome().getId()));
    assertNotNull(net_client.getNode(net_router.getHome().getId()));

    final AtomicInteger counter = new AtomicInteger(0);
    Supplier<Integer> sup = () -> {
      return counter.incrementAndGet();
    };

    ProcedureInformation counterProcedure = ProcedureInformation.of("counter", sup);
    counterProcedure.getProcedure(function)
    net_client.getProcedureManager().registerProcedure("counter", sup);
    net_router.getProcedureManager().



  }

}
