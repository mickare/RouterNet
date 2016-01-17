package de.rennschnitzel.backbone.net;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import de.rennschnitzel.backbone.Owner;
import de.rennschnitzel.backbone.net.channel.Channel;
import de.rennschnitzel.backbone.net.channel.ChannelDescriptors;
import de.rennschnitzel.backbone.net.channel.object.ConvertObjectChannelException;
import de.rennschnitzel.backbone.net.channel.object.ObjectChannel;
import de.rennschnitzel.backbone.net.channel.stream.StreamChannel;
import de.rennschnitzel.backbone.net.dummy.DummyConnection;
import de.rennschnitzel.backbone.net.dummy.DummyNetwork;
import de.rennschnitzel.backbone.net.packet.BasePacketHandler;

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

    testingOwner = new Owner() {
      @Override
      public Logger getLogger() {
        return Logger.getLogger("ChannelTest");
      }
      @Override
      public String getName() {
        return "ChannelTestOwner";
      }
    };

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

    Channel base0 = con_client.getChannel("base0");
    assertTrue(con_router.getChannelIfPresent("base0").getChannelId() == base0.getChannelId());
    Channel base1 = con_router.getChannel("base1");
    assertTrue(con_client.getChannelIfPresent("base1").getChannelId() == base1.getChannelId());

    byte[] data0 = new byte[128];
    byte[] data1 = new byte[128];
    rand.nextBytes(data0);
    rand.nextBytes(data1);

    final AtomicInteger rec_client = new AtomicInteger(0);
    final AtomicInteger rec_router = new AtomicInteger(0);

    con_client.getChannelIfPresent("base0").registerMessageListener(testingOwner, (msg) -> {
      assertArrayEquals(data0, msg.getData().toByteArray());
      rec_client.incrementAndGet();
    });

    con_router.getChannelIfPresent("base1").registerMessageListener(testingOwner, (msg) -> {
      assertArrayEquals(data1, msg.getData().toByteArray());
      rec_router.incrementAndGet();
    });


    con_client.getChannelIfPresent("base0").send(target_client, data0);
    assertEquals(1, rec_client.get());
    con_client.getChannelIfPresent("base0").send(target_router, data0); // does nothing
    assertEquals(0, rec_router.get());
    assertEquals(1, rec_client.get());
    con_client.getChannelIfPresent("base1").send(target_client, data1); // does nothing
    assertEquals(1, rec_client.get());
    con_client.getChannelIfPresent("base1").send(target_router, data1);
    assertEquals(1, rec_router.get());

    con_router.getChannelIfPresent("base0").send(target_client, data0);
    assertEquals(2, rec_client.get());
    con_router.getChannelIfPresent("base0").send(target_router, data0); // does nothing
    assertEquals(1, rec_router.get());
    assertEquals(2, rec_client.get());
    con_router.getChannelIfPresent("base1").send(target_client, data1); // does nothing
    assertEquals(2, rec_client.get());
    con_router.getChannelIfPresent("base1").send(target_router, data1);
    assertEquals(2, rec_router.get());

    con_router.getChannelIfPresent("base1").registerMessageListener(testingOwner, (msg) -> {
      assertEquals(con_client.getNetwork().getHome().getId(), msg.getSender());
    });
    con_client.getChannelIfPresent("base1").send(target_router, data1);

  }

  @Test
  public void testObjectChannel() throws ConvertObjectChannelException, IOException {

    ObjectChannel.Descriptor<String> desc = ChannelDescriptors.getObjectChannel("object", String.class);

    ObjectChannel<String> ch_client = con_client.getChannel(desc);
    ObjectChannel<String> ch_router = con_router.getChannel(desc);

    byte[] data = new byte[128];
    rand.nextBytes(data);

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < data.length; ++i) {
      sb.append(Integer.toUnsignedString(data[i], 16));
    }
    String testData = sb.toString();

    ch_router.registerMessageListener(testingOwner, msg -> {
      assertEquals(net_client.getHome().getId(), msg.getSender());
      assertEquals(testData, msg.getObject());
    });

    ch_client.send(Target.to(net_router.getHome().getId()), testData);

  }

  @Test
  public void testStreamChannel() throws IOException {

    StreamChannel.Descriptor descIn = ChannelDescriptors.getStreamChannel("stream");
    StreamChannel.Descriptor descOut = ChannelDescriptors.getStreamChannel("stream");

    StreamChannel client = con_client.getChannel(descOut);
    StreamChannel router = con_router.getChannel(descIn);

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
