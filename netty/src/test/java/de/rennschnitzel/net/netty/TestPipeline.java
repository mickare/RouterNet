package de.rennschnitzel.net.netty;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.net.HostAndPort;

import de.rennschnitzel.net.Owner;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.Node;
import de.rennschnitzel.net.core.Target;
import de.rennschnitzel.net.core.Tunnel;
import de.rennschnitzel.net.core.login.AuthenticationClient;
import de.rennschnitzel.net.core.login.AuthenticationFactory;
import de.rennschnitzel.net.core.login.AuthenticationRouter;
import de.rennschnitzel.net.core.login.LoginHandler;
import de.rennschnitzel.net.core.packet.BasePacketHandler;
import de.rennschnitzel.net.core.packet.PacketHandler;
import de.rennschnitzel.net.core.procedure.BoundProcedure;
import de.rennschnitzel.net.core.procedure.Procedure;
import de.rennschnitzel.net.dummy.DummClientNetwork;
import de.rennschnitzel.net.dummy.DummyLogger;
import de.rennschnitzel.net.netty.login.NettyLoginClientHandler;
import de.rennschnitzel.net.netty.login.NettyLoginRouterHandler;
import de.rennschnitzel.net.util.SimpleOwner;
import de.rennschnitzel.net.util.concurrent.DirectScheduledExecutorService;
import io.netty.channel.ChannelHandlerContext;

public class TestPipeline {

  private Logger logger = new DummyLogger("TestPipeline", System.out);

  private AuthenticationRouter authRouter = AuthenticationFactory.newPasswordForRouter("test");
  private AuthenticationClient authClient = AuthenticationFactory.newPasswordForClient("test");

  private Owner testingOwner;
  private DummClientNetwork net_router;
  private DummClientNetwork net_client;

  private NettyServer server;
  private HostAndPort serverAddress = HostAndPort.fromParts("localhost", 10000);

  private NettyClient client;

  private void log(String msg) {
    logger.info(msg);
  }

  @Before
  public void setup() throws Exception {
    DirectScheduledExecutorService.disableWarning();
    log("setup start");

    long start = System.currentTimeMillis();

    testingOwner = new SimpleOwner("ChannelTestOwner", logger);


    // NETWORKS
    net_router = new DummClientNetwork();
    net_router.setName("Router");
    net_client = new DummClientNetwork(net_router.newNotUsedUUID());
    net_client.setName("Client");


    // ROUTER - HANDLERS
    Supplier<LoginHandler<ChannelHandlerContext>> router_loginHandler =
        () -> new NettyLoginRouterHandler(net_router, authRouter);
    Supplier<PacketHandler<NettyConnection<DummClientNetwork>>> router_packetHandler =
        () -> new BasePacketHandler<NettyConnection<DummClientNetwork>>();

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
    client.connect().get(1, TimeUnit.SECONDS);
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
  public void testNodes()
      throws IOException, InterruptedException, TimeoutException, ExecutionException {
    log("testNodes start");
    long start = System.currentTimeMillis();

    // Test Node
    assertNotNull(net_router.getNode(net_client.getHome().getId()));
    assertNotNull(net_client.getNode(net_router.getHome().getId()));

    final AtomicInteger counter = new AtomicInteger(0);
    Supplier<Integer> sup = () -> {
      return counter.incrementAndGet();
    };

    BoundProcedure<Void, Integer> counterProcedure = Procedure.of("counter", sup, net_router);
    net_client.getProcedureManager().registerProcedure(counterProcedure);
    assertNotNull(net_client.getProcedureManager().getRegisteredProcedure(counterProcedure));
    assertTrue(net_client.getHome().getProcedures().contains(counterProcedure));


    net_client.getHome().sendUpdate(net_client).addListener(f -> {
      if (f.isSuccess()) {
        log("publish success");
      } else {
        logger.log(Level.SEVERE, "publish failed", f.cause());
      }
    }).await(500, TimeUnit.MILLISECONDS);

    Node node_client = net_router.getNode(net_client.getHome().getId());

    log("procedures: " + node_client.getProcedures().toString());

    Thread.sleep(10);
    assertTrue(node_client.hasProcedure(counterProcedure));

    assertEquals(1, counterProcedure.call(node_client, null) //
        .get(100, TimeUnit.MILLISECONDS).intValue());
    assertEquals(2, counterProcedure.call(node_client, null) //
        .get(100, TimeUnit.MILLISECONDS).intValue());
    assertEquals(3, counterProcedure.call(node_client, null) //
        .get(100, TimeUnit.MILLISECONDS).intValue());
    assertEquals(4, counterProcedure.call(node_client, null) //
        .get(100, TimeUnit.MILLISECONDS).intValue());


    log("testNodes complete (took " + (System.currentTimeMillis() - start) + "ms)");
  }

  private final Random rand = new Random();

  @Test
  public void testTunnel()
      throws IOException, InterruptedException, ExecutionException, TimeoutException {

    Connection con_router = net_router.getConnectionFuture().get(1, TimeUnit.SECONDS);
    Connection con_client = net_client.getConnectionFuture().get(1, TimeUnit.SECONDS);

    Target target_client = Target.to(net_client.getHome().getId());
    Target target_router = Target.to(net_router.getHome().getId());

    // copied from tunnelTest

    Tunnel base0 = net_client.getTunnel("base0");
    base0.register().await(100);
    // assertTrue(con_client.registerTunnel(base0).await(1, TimeUnit.SECONDS));
    assertNotNull(con_client.getTunnelId(base0));
    assertNotNull(con_router.getTunnelIdIfPresent(base0));
    assertTrue(con_router.getTunnelIdIfPresent(base0) == con_client.getTunnelIdIfPresent(base0));
    Tunnel base1 = net_router.getTunnel("base1");
    Thread.sleep(5);
    assertTrue(con_router.getTunnelIdIfPresent(base1) == con_client.getTunnelIdIfPresent(base1));

    // Should not be initialized but be registered.
    assertNull(net_client.getTunnelIfPresent("base1"));
    assertNull(net_router.getTunnelIfPresent("base0"));
    assertEquals(con_client.getTunnelIdIfPresent(base0), con_router.getTunnelIdIfPresent(base0));
    assertEquals(con_router.getTunnelIdIfPresent(base1), con_client.getTunnelIdIfPresent(base1));


    // Initialize the tunnels on the other side
    net_client.getTunnel("base1");
    net_router.getTunnel("base0");
    assertEquals(net_client.getTunnelIfPresent("base0").getName(),
        net_router.getTunnelIfPresent("base0").getName());
    assertEquals(net_router.getTunnelIfPresent("base1").getName(),
        net_client.getTunnelIfPresent("base1").getName());

    byte[] data0 = new byte[128];
    byte[] data1 = new byte[128];
    rand.nextBytes(data0);
    rand.nextBytes(data1);

    final AtomicInteger rec_client = new AtomicInteger(0);
    final AtomicInteger rec_router = new AtomicInteger(0);

    net_client.getTunnelIfPresent("base0").registerMessageListener(testingOwner, (msg) -> {
      assertArrayEquals(data0, msg.getData().toByteArray());
      rec_client.incrementAndGet();
    });

    net_router.getTunnelIfPresent("base1").registerMessageListener(testingOwner, (msg) -> {
      assertArrayEquals(data1, msg.getData().toByteArray());
      rec_router.incrementAndGet();
    });

    net_client.getTunnelIfPresent("base0").send(target_client, data0);
    Thread.sleep(5);
    assertEquals(1, rec_client.get());
    net_client.getTunnelIfPresent("base0").send(target_router, data0); // does nothing
    Thread.sleep(5);
    assertEquals(0, rec_router.get());
    assertEquals(1, rec_client.get());
    net_client.getTunnelIfPresent("base1").send(target_client, data1); // does nothing
    Thread.sleep(5);
    assertEquals(1, rec_client.get());
    net_client.getTunnelIfPresent("base1").send(target_router, data1);
    Thread.sleep(5);
    assertEquals(1, rec_router.get());

    net_router.getTunnelIfPresent("base0").send(target_client, data0);
    Thread.sleep(5);
    assertEquals(2, rec_client.get());
    net_router.getTunnelIfPresent("base0").send(target_router, data0); // does nothing
    Thread.sleep(5);
    assertEquals(1, rec_router.get());
    assertEquals(2, rec_client.get());
    net_router.getTunnelIfPresent("base1").send(target_client, data1); // does nothing
    Thread.sleep(5);
    assertEquals(2, rec_client.get());
    net_router.getTunnelIfPresent("base1").send(target_router, data1);
    Thread.sleep(5);
    assertEquals(2, rec_router.get());

    net_router.getTunnelIfPresent("base1").registerMessageListener(testingOwner, (msg) -> {
      assertEquals(con_client.getNetwork().getHome().getId(), msg.getSenderId());
    });
    Thread.sleep(5);
    net_client.getTunnelIfPresent("base1").send(target_router, data1);

  }

}
