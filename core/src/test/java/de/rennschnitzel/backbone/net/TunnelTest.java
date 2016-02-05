package de.rennschnitzel.backbone.net;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.Owner;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.Target;
import de.rennschnitzel.net.core.Tunnel;
import de.rennschnitzel.net.core.login.AuthenticationFactory;
import de.rennschnitzel.net.core.login.ClientLoginEngine;
import de.rennschnitzel.net.core.login.RouterLoginEngine;
import de.rennschnitzel.net.core.packet.BasePacketHandler;
import de.rennschnitzel.net.core.tunnel.TunnelDescriptors;
import de.rennschnitzel.net.core.tunnel.object.ConvertObjectTunnelException;
import de.rennschnitzel.net.core.tunnel.object.ObjectTunnel;
import de.rennschnitzel.net.core.tunnel.stream.StreamTunnel;
import de.rennschnitzel.net.dummy.DummClientNetwork;
import de.rennschnitzel.net.netty.ConnectionHandler;
import de.rennschnitzel.net.netty.LocalConnectClient;
import de.rennschnitzel.net.netty.LoginHandler;
import de.rennschnitzel.net.netty.PipelineUtils;
import de.rennschnitzel.net.service.ConnectClient;
import de.rennschnitzel.net.util.FutureUtils;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Promise;

public class TunnelTest {

  private final Random rand = new Random();

  Owner testingOwner;

  DummClientNetwork net_router;
  DummClientNetwork net_client;

  Target target_client;
  Target target_router;

  private LocalConnectClient con;
  EventLoopGroup group = new DefaultEventLoopGroup();

  @Before
  public void setup() throws InterruptedException {

    testingOwner = new Owner() {
      @Override
      public Logger getLogger() {
        return Logger.getLogger("ProcedureTest");
      }

      @Override
      public String getName() {
        return "ProcedureTestOwner";
      }
    };

    net_router = new DummClientNetwork(group, new UUID(0, 1));
    net_router.setName("Router");
    net_client = new DummClientNetwork(group, new UUID(0, 2));
    net_client.setName("Client");

    RouterLoginEngine routerEngine = new RouterLoginEngine(net_router, AuthenticationFactory.newPasswordForRouter("pw"));
    ClientLoginEngine clientEngine = new ClientLoginEngine(net_client, AuthenticationFactory.newPasswordForClient("pw"));

    target_client = Target.to(net_client.getHome().getId());
    target_router = Target.to(net_router.getHome().getId());

    final Promise<Connection> con_router = FutureUtils.newPromise();
    final Promise<Connection> con_client = FutureUtils.newPromise();
    
    con = new LocalConnectClient(PipelineUtils.baseInitAnd(ch -> {
      ch.pipeline().addLast(new LoginHandler(routerEngine, con_router));
      ch.pipeline().addLast(new ConnectionHandler(net_router, new BasePacketHandler()));
    }), PipelineUtils.baseInitAnd(ch -> {
      ch.pipeline().addLast(new LoginHandler(clientEngine, con_client));
      ch.pipeline().addLast(new ConnectionHandler(net_client, new BasePacketHandler()));
    }), group);

    con.connect();
    con.awaitRunning();
    Preconditions.checkState(con.getState() == ConnectClient.State.ACTIVE);

    net_client.awaitConnected(1, TimeUnit.SECONDS);
    net_router.awaitConnected(1, TimeUnit.SECONDS);

    Preconditions.checkNotNull(net_client.getNode(net_router.getHome().getId()));
    Preconditions.checkNotNull(net_router.getNode(net_client.getHome().getId()));

  }

  @After
  public void tearDown() {
    con.close();
    group.shutdownGracefully(1, 100, TimeUnit.MILLISECONDS);
  }

  private boolean busyWaiting(final int tries, final Callable<Boolean> condition, final long millis) throws Exception {
    int _try = 0;
    boolean result = condition.call();
    while (_try++ < tries && !result) {
      Thread.sleep(millis);
      result = condition.call();
    }
    return result;
  }

  private void assertWaiting(final int tries, final Callable<Boolean> condition, final long millis) throws Exception {
    assertTrue(busyWaiting(tries, condition, millis));
  }

  @Test
  public void testTunnel() throws Exception {

    Tunnel base0 = net_client.getTunnel("base0");
    Tunnel base1 = net_router.getTunnel("base1");

    assertWaiting(10, () -> net_client.getTunnelIfPresent("base1") != null, 100);

    // Should not be initialized but be registered.
    Tunnel base1_client = net_client.getTunnelIfPresent("base1");
    Tunnel base0_router = net_router.getTunnelIfPresent("base0");
    assertNotNull(base1_client);
    assertNotNull(base0_router);
    assertEquals(base1.getId(), base1_client.getId());
    assertEquals(base0.getId(), base0_router.getId());


    // Initialize the tunnels on the other side
    net_client.getTunnel("base1");
    net_router.getTunnel("base0");
    assertEquals(net_client.getTunnelIfPresent("base0").getName(), net_router.getTunnelIfPresent("base0").getName());
    assertEquals(net_router.getTunnelIfPresent("base1").getName(), net_client.getTunnelIfPresent("base1").getName());

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
    assertWaiting(10, () -> 1 == rec_client.get(), 100);
    assertEquals(1, rec_client.get());
    net_client.getTunnelIfPresent("base0").send(target_router, data0); // does nothing
    assertEquals(0, rec_router.get());
    assertEquals(1, rec_client.get());
    net_client.getTunnelIfPresent("base1").send(target_client, data1); // does nothing
    assertEquals(1, rec_client.get());
    net_client.getTunnelIfPresent("base1").send(target_router, data1);
    assertWaiting(10, () -> 1 == rec_router.get(), 100);
    // assertEquals(1, rec_router.get());

    net_router.getTunnelIfPresent("base0").send(target_client, data0);
    assertWaiting(10, () -> 2 == rec_client.get(), 100);
    net_router.getTunnelIfPresent("base0").send(target_router, data0); // does nothing
    assertEquals(1, rec_router.get());
    assertEquals(2, rec_client.get());
    net_router.getTunnelIfPresent("base1").send(target_client, data1); // does nothing
    assertEquals(2, rec_client.get());
    net_router.getTunnelIfPresent("base1").send(target_router, data1);
    assertWaiting(10, () -> 2 == rec_router.get(), 100);
    assertEquals(2, rec_router.get());

    net_router.getTunnelIfPresent("base1").registerMessageListener(testingOwner, (msg) -> {
      assertEquals(net_client.getHome().getId(), msg.getSenderId());
    });

    net_client.getTunnelIfPresent("base1").send(target_router, data1);

  }

  @Test
  public void testObjectTunnel() throws ConvertObjectTunnelException, IOException {

    ObjectTunnel.Descriptor<String> desc = TunnelDescriptors.getObjectTunnel("object", String.class);

    ObjectTunnel<String> ch_client = net_client.getTunnel(desc);
    ObjectTunnel<String> ch_router = net_router.getTunnel(desc);

    byte[] data = new byte[128];
    rand.nextBytes(data);

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < data.length; ++i) {
      sb.append(Integer.toUnsignedString(data[i], 16));
    }
    String testData = sb.toString();

    ch_router.registerMessageListener(testingOwner, msg -> {
      assertEquals(net_client.getHome().getId(), msg.getSenderId());
      assertEquals(testData, msg.getObject());
    });

    ch_client.send(Target.to(net_router.getHome().getId()), testData);

  }

  @Test
  public void testStreamTunnel() throws Exception {

    StreamTunnel.Descriptor descIn = TunnelDescriptors.getStreamTunnel("stream");
    StreamTunnel.Descriptor descOut = TunnelDescriptors.getStreamTunnel("stream");

    StreamTunnel client = net_client.getTunnel(descOut);
    StreamTunnel router = net_router.getTunnel(descIn);
    
    try (InputStream routerIn = router.newInputBuffer()) {
      try (InputStream clientIn = client.newInputBuffer()) {

        byte[][] data = new byte[10][128];

        try (OutputStream out = client.newOutputBuffer(Target.toAll())) {
          for (int i = 0; i < data.length; ++i) {
            rand.nextBytes(data[i]);
            out.write(data[i]);
          }
        }

        // To All so both should get something
        for (int i = 0; i < data.length; ++i) {
          byte[] buf = new byte[128];
          routerIn.read(buf);
          assertArrayEquals(data[i], buf);
        }
        for (int i = 0; i < data.length; ++i) {
          byte[] buf = new byte[128];
          clientIn.read(buf);
          assertArrayEquals(data[i], buf);
        }

        try (OutputStream out = client.newOutputBuffer(Target.to(net_router.getHome()))) {
          for (int i = 0; i < data.length; ++i) {
            rand.nextBytes(data[i]);
            out.write(data[i]);
          }
        }
        // Only to router
        for (int i = 0; i < data.length; ++i) {
          byte[] buf = new byte[128];
          routerIn.read(buf);
          assertArrayEquals(data[i], buf);
        }
        assertTrue(clientIn.available() == 0);

      }
    }

    try (InputStream clientIn = client.newInputBuffer()) {
      assertTrue(clientIn.available() == 0);
    }

    try (InputStream routerIn = router.newInputBuffer()) {
      assertTrue(routerIn.available() == 0);
    }

    client.close();
    router.close();

    assertTrue(router.isClosed());
    assertTrue(client.isClosed());

  }

}
