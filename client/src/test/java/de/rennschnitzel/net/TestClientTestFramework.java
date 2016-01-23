package de.rennschnitzel.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.Tunnel;
import de.rennschnitzel.net.dummy.DummClientNetwork;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeMessage;
import de.rennschnitzel.net.util.SimpleOwner;
import de.rennschnitzel.net.util.concurrent.DirectScheduledExecutorService;

public class TestClientTestFramework {

  private Owner owner;
  private NetClient client;

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    owner = new SimpleOwner("TestNetClient", Logger.getLogger("TestNetClient"));
    client = new NetClient(NodeMessage.Type.BUKKIT);

    folder.create();
    client.init(Logger.getLogger("TestNetClient"), folder.newFolder("net"),
        new DirectScheduledExecutorService());

    client.enable();
  }

  @After
  public void tearDown() throws Exception {
    client.disable();
    folder.delete();
  }


  @Test
  public void testTunnel() throws Exception {

    assertNotNull(Net.getNetwork());
    assertNotNull(client.getTestFramework());

    final AtomicInteger received = new AtomicInteger(0);

    DummClientNetwork net_router = client.getTestFramework().getRouterNetwork();
    Network net_client = client.getNetwork();

    Connection con_router = net_router.getConnectionFuture().get(1, TimeUnit.SECONDS);
    Connection con_client = net_client.getConnectionFuture().get(1, TimeUnit.SECONDS);

    Tunnel tunnel = net_router.getTunnel("testTunnel");
    tunnel.register().await(100);

    tunnel.registerMessageListener(owner, msg -> {
      received.addAndGet(msg.getData().byteAt(0));
    });

    assertNotNull(con_client.getTunnelIdIfPresent("testTunnel"));

    Net.getTunnel("testTunnel").broadcast(new byte[] {1});
    assertEquals(1, received.get());

    Net.getTunnel("testTunnel").broadcast(new byte[] {2});
    assertEquals(3, received.get());

  }


}
