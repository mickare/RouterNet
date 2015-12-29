package de.rennschnitzel.backbone.net.store;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import de.rennschnitzel.backbone.net.Connection;
import de.rennschnitzel.backbone.net.ConnectionForTesting;
import de.rennschnitzel.backbone.net.NetworkForTesting;
import de.rennschnitzel.backbone.net.node.HomeNode;
import de.rennschnitzel.backbone.net.packet.PacketHandler;
import de.rennschnitzel.backbone.net.packet.SimplePacketHandler;
import de.rennschnitzel.backbone.net.protocol.DataStoreProtocol.DataStoreRequestMessage;
import de.rennschnitzel.backbone.net.protocol.DataStoreProtocol.DataStoreResponseMessage;

public class DataStoreTest {

  private final Random rand = new Random();

  HomeNode home;
  LocalDataStore local;
  RemoteDataStore remote;
  ConnectionForTesting con;
  NetworkForTesting net;

  @Before
  public void setup() {

    Logger logger = Logger.getLogger("DataStoreTest");

    BaseDataStore base = new BaseDataStore();
    local = new LocalDataStore(base);
    NetworkDataStoreHandler storeHandler = new NetworkDataStoreHandler(logger, base);

    PacketHandler<Connection> packetHandler = new SimplePacketHandler<Connection>() {
      @Override
      public void handle(Connection ctx, DataStoreResponseMessage msg) throws Exception {
        remote.handle(ctx, msg);
      }

      @Override
      public void handle(Connection ctx, DataStoreRequestMessage msg) throws Exception {
        storeHandler.handle(ctx, msg);
      }

    };

    home = new HomeNode(UUID.randomUUID());
    net = new NetworkForTesting(local);
    con = new ConnectionForTesting(net, home, packetHandler);
    remote = new RemoteDataStore(con);
    net.setConnection(con);
    net.setInstance();
  }

  @Test
  public void add() throws InterruptedException, ExecutionException {

    EntryKey key = new EntryKey("test_add");
    byte[][] data = new byte[10][64];
    for (int i = 0; i < data.length; ++i) {
      rand.nextBytes(data[i]);
    }
    for (int i = 0; i < data.length; ++i) {
      assertNull(remote.add(key, data[i]).get());
      assertArrayEquals(local.getBase().get(key).get(i), data[i]);
    }

    for (int i = 0; i < data.length; ++i) {
      assertArrayEquals(remote.get(key, i).get().toByteArray(), data[i]);
    }

    remote.clear(key);

    assertTrue(local.getBase().isEmpty());

  }

  @Test
  public void push() throws InterruptedException, ExecutionException {

    EntryKey key = new EntryKey("test_push");
    byte[][] data = new byte[10][64];
    for (int i = 0; i < data.length; ++i) {
      rand.nextBytes(data[i]);
    }

    for (int i = 0; i < data.length; ++i) {
      assertNull(remote.push(key, data[i]).get());
      assertArrayEquals(local.getBase().get(key).get(0), data[i]);
    }

    for (int i = 0; i < data.length; ++i) {
      assertArrayEquals(remote.get(key, i).get().toByteArray(), data[data.length - 1 - i]);
    }

    for (int i = 0; i < data.length; ++i) {
      assertArrayEquals(remote.pop(key).get().get().toByteArray(), data[data.length - 1 - i]);
    }

    assertTrue(local.getBase().isEmpty());

  }

  @Test
  public void remove() throws InterruptedException, ExecutionException {

    EntryKey key = new EntryKey("test_push");
    List<byte[]> data = Lists.newArrayList();
    for (int i = 0; i < 100; ++i) {
      data.add(ByteBuffer.allocate(4).putInt(i).array());
    }

    remote.set(key, data);

    for (int i = 0; i < data.size(); ++i) {
      assertArrayEquals(remote.get(key, i).get().toByteArray(), data.get(i));
    }

    for (int i = 0; i < data.size(); ++i) {
      //assertEquals(local.getBase().remove(key, ImmutableList.of(data.get(i))), 1);
      assertEquals(remote.remove(key, data.get(i)).get().intValue(), 1);
    }

    assertTrue(local.getBase().isEmpty());

  }

  @After
  public void teardown() {

  }


}
