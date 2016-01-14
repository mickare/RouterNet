package de.rennschnitzel.backbone.net;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
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
import de.rennschnitzel.backbone.net.packet.BasePacketHandler;

public class ChannelTest {

  private final Random rand = new Random();

  Owner testingOwner;

  NetworkForTesting net_router;
  NetworkForTesting net_client;

  ConnectionForTesting con_router;
  ConnectionForTesting con_client;

  Target target_client;
  Target target_router;

  @Before
  public void setup() {

    testingOwner = new Owner() {
      @Override
      public Logger getLogger() {
        return Logger.getLogger("ChannelTest");
      }
    };

    net_router = new NetworkForTesting();
    net_client = new NetworkForTesting();

    con_router = new ConnectionForTesting(net_router, new BasePacketHandler<>());
    con_client = new ConnectionForTesting(net_client, new BasePacketHandler<>());


    target_client = Target.to(net_client.getHome().getId());
    target_router = Target.to(net_router.getHome().getId());

    con_router.connect(con_client);

  }



  @Test
  public void testChannel() throws IOException {

    Channel base0 = con_client.getOrCreateChannel("base0");
    assertTrue(con_router.getChannel("base0").getChannelId() == base0.getChannelId());
    Channel base1 = con_router.getOrCreateChannel("base1");
    assertTrue(con_client.getChannel("base1").getChannelId() == base1.getChannelId());

    byte[] data0 = new byte[128];
    byte[] data1 = new byte[128];
    rand.nextBytes(data0);
    rand.nextBytes(data1);

    final AtomicInteger rec_client = new AtomicInteger(0);
    final AtomicInteger rec_router = new AtomicInteger(0);

    con_client.getChannel("base0").registerMessageListener(testingOwner, (msg) -> {
      assertArrayEquals(data0, msg.getData().toByteArray());
      rec_client.incrementAndGet();
    });

    con_router.getChannel("base1").registerMessageListener(testingOwner, (msg) -> {
      assertArrayEquals(data1, msg.getData().toByteArray());
      rec_router.incrementAndGet();
    });


    con_client.getChannel("base0").send(target_client, data0);
    assertEquals(1, rec_client.get());
    con_client.getChannel("base0").send(target_router, data0); // does nothing
    assertEquals(0, rec_router.get());
    assertEquals(1, rec_client.get());
    con_client.getChannel("base1").send(target_client, data1); // does nothing
    assertEquals(1, rec_client.get());
    con_client.getChannel("base1").send(target_router, data1);
    assertEquals(1, rec_router.get());

    con_router.getChannel("base0").send(target_client, data0);
    assertEquals(2, rec_client.get());
    con_router.getChannel("base0").send(target_router, data0); // does nothing
    assertEquals(1, rec_router.get());
    assertEquals(2, rec_client.get());
    con_router.getChannel("base1").send(target_client, data1); // does nothing
    assertEquals(2, rec_client.get());
    con_router.getChannel("base1").send(target_router, data1);
    assertEquals(2, rec_router.get());

    con_router.getChannel("base1").registerMessageListener(testingOwner, (msg) -> {
      assertEquals(con_client.getHome().getId(), msg.getSender());
    });
    con_client.getChannel("base1").send(target_router, data1);

  }

  @Test
  public void testObjectChannel() throws ConvertObjectChannelException, IOException {

    ObjectChannel.Descriptor<String> desc = ChannelDescriptors.getObjectChannel("object", String.class);

    ObjectChannel<String> ch_client = con_client.getOrCreateSubChannel(desc, testingOwner);
    ObjectChannel<String> ch_router = con_router.getOrCreateSubChannel(desc, testingOwner);

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

    StreamChannel out = con_client.getOrCreateSubChannel(descOut, testingOwner);
    StreamChannel in = con_router.getOrCreateSubChannel(descIn, testingOwner);

    InputStream input = in.newInputBuffer();

    byte[][] data = new byte[10][128];
    for (int i = 0; i < data.length; ++i) {
      rand.nextBytes(data[i]);
      out.getOutputBuffer().write(data[i]);
    }

    out.getOutputBuffer().flush();

    for (int i = 0; i < data.length; ++i) {
      byte[] buf = new byte[128];
      input.read(buf);
      assertArrayEquals(buf, data[i]);
    }

    out.close();
    in.close();

    assertTrue(in.isClosed());
    assertTrue(out.isClosed());

  }

}
