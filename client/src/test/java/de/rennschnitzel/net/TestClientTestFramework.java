package de.rennschnitzel.net;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.util.concurrent.MoreExecutors;

import de.rennschnitzel.net.core.Tunnel;
import de.rennschnitzel.net.dummy.DummClientNetwork;
import de.rennschnitzel.net.dummy.DummyLogger;
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
    client.init(new DummyLogger("Client", System.out), folder.newFolder("net"),
        MoreExecutors.listeningDecorator(new DirectScheduledExecutorService()));

    client.enable();
    client.getConnectService().getCurrentFuture().await(1000);
    assertTrue(client.getConnectService().getCurrentFuture().isSuccess());
  }

  @After
  public void tearDown() throws Exception {
    client.disable();
    folder.delete();
  }


  private boolean busyWaiting(final int tries, final Callable<Boolean> condition, final long millis)
      throws Exception {
    int _try = 0;
    boolean result = condition.call();
    while (_try++ < tries && !result) {
      Thread.sleep(millis);
      result = condition.call();
    }
    return result;
  }

  private void assertWaiting(final int tries, final Callable<Boolean> condition, final long millis)
      throws Exception {
    assertTrue(busyWaiting(tries, condition, millis));
  }

  @Test
  public void testTunnel() throws Exception {

    assertNotNull(Net.getNetwork());
    assertNotNull(client.getTestFramework());

    final AtomicInteger received = new AtomicInteger(0);

    DummClientNetwork net_router = client.getTestFramework().getRouterNetwork();
    Network net_client = client.getNetwork();

    Tunnel tunnel = net_router.getTunnel("testTunnel");
    tunnel.registerMessageListener(owner, msg -> {
      received.addAndGet(msg.getData().byteAt(0));
    });

    assertWaiting(5, () -> net_client.getTunnel("testTunnel") != null, 100);

    Thread.sleep(100);

    assertTrue(Net.getTunnel("testTunnel").broadcast(new byte[] {1}));
    assertWaiting(5, () -> received.get() == 1, 100);

    assertTrue(Net.getTunnel("testTunnel").broadcast(new byte[] {2}));
    assertWaiting(5, () -> received.get() == 3, 100);

  }


}
