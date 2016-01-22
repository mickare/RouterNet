package de.rennschnitzel.backbone.net;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import de.rennschnitzel.net.Owner;
import de.rennschnitzel.net.core.Tunnel;
import de.rennschnitzel.net.core.Target;
import de.rennschnitzel.net.core.packet.BasePacketHandler;
import de.rennschnitzel.net.core.tunnel.TunnelDescriptors;
import de.rennschnitzel.net.core.tunnel.object.ConvertObjectChannelException;
import de.rennschnitzel.net.core.tunnel.object.ObjectTunnel;
import de.rennschnitzel.net.core.tunnel.stream.StreamTunnel;
import de.rennschnitzel.net.dummy.DummyConnection;
import de.rennschnitzel.net.dummy.DummyNetwork;
import de.rennschnitzel.net.util.SimpleOwner;

public class ChannelTest {

  private final Random rand = new Random();

  Owner testingOwner;

  DummyNetwork net_router;
  DummyNetwork net_client;

  DummyConnection con_router;
  DummyConnection con_client;

  Target target_client;
  Target target_router;

  @Before
  public void setup() {

    testingOwner = new SimpleOwner("ChannelTestOwner", Logger.getLogger("ChannelTest"));

    net_router = new DummyNetwork();
    do {
      net_client = new DummyNetwork();
    } while (net_client.getHome().getId().equals(net_router.getHome().getId()));

    con_router = new DummyConnection(net_router, new BasePacketHandler<>());
    con_client = new DummyConnection(net_client, new BasePacketHandler<>());

    target_client = Target.to(net_client.getHome().getId());
    target_router = Target.to(net_router.getHome().getId());

    con_router.connect(con_client);

  }



  @Test
  public void testChannel() throws IOException {

    Tunnel base0 = net_client.getTunnel("base0");
    assertNotNull(con_router.getTunnelIdIfPresent(base0));
    assertNotNull(con_client.getTunnelIdIfPresent(base0));
    assertTrue(con_router.getTunnelIdIfPresent(base0) == con_client.getTunnelIdIfPresent(base0));
    Tunnel base1 = net_router.getTunnel("base1");
    assertTrue(con_router.getTunnelIdIfPresent(base1) == con_client.getTunnelIdIfPresent(base1));

    // Should not be initialized but be registered.
    assertNull(net_client.getTunnelIfPresent("base1"));
    assertNull(net_router.getTunnelIfPresent("base0"));
    assertEquals(con_client.getTunnelIdIfPresent(base0), con_router.getTunnelIdIfPresent(base0));
    assertEquals(con_router.getTunnelIdIfPresent(base1), con_client.getTunnelIdIfPresent(base1));


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
    assertEquals(1, rec_client.get());
    net_client.getTunnelIfPresent("base0").send(target_router, data0); // does nothing
    assertEquals(0, rec_router.get());
    assertEquals(1, rec_client.get());
    net_client.getTunnelIfPresent("base1").send(target_client, data1); // does nothing
    assertEquals(1, rec_client.get());
    net_client.getTunnelIfPresent("base1").send(target_router, data1);
    assertEquals(1, rec_router.get());

    net_router.getTunnelIfPresent("base0").send(target_client, data0);
    assertEquals(2, rec_client.get());
    net_router.getTunnelIfPresent("base0").send(target_router, data0); // does nothing
    assertEquals(1, rec_router.get());
    assertEquals(2, rec_client.get());
    net_router.getTunnelIfPresent("base1").send(target_client, data1); // does nothing
    assertEquals(2, rec_client.get());
    net_router.getTunnelIfPresent("base1").send(target_router, data1);
    assertEquals(2, rec_router.get());

    net_router.getTunnelIfPresent("base1").registerMessageListener(testingOwner, (msg) -> {
      assertEquals(con_client.getNetwork().getHome().getId(), msg.getSenderId());
    });
    net_client.getTunnelIfPresent("base1").send(target_router, data1);

  }

  @Test
  public void testObjectChannel() throws ConvertObjectChannelException, IOException {

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
  public void testStreamChannel() throws IOException {

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
          assertArrayEquals(buf, data[i]);
        }
        for (int i = 0; i < data.length; ++i) {
          byte[] buf = new byte[128];
          clientIn.read(buf);
          assertArrayEquals(buf, data[i]);
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
          assertArrayEquals(buf, data[i]);
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
