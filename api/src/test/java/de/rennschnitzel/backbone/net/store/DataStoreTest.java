package de.rennschnitzel.backbone.net.store;

import java.util.UUID;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.rennschnitzel.backbone.net.Connection;
import de.rennschnitzel.backbone.net.ConnectionForTesting;
import de.rennschnitzel.backbone.net.NetworkForTesting;
import de.rennschnitzel.backbone.net.node.HomeNode;
import de.rennschnitzel.backbone.net.packet.PacketHandler;
import de.rennschnitzel.backbone.net.packet.SimplePacketHandler;
import de.rennschnitzel.backbone.net.protocol.DataStoreProtocol.DataStoreRequestMessage;
import de.rennschnitzel.backbone.net.protocol.DataStoreProtocol.DataStoreResponseMessage;

public class DataStoreTest {

  HomeNode home;
  RouterDataStore router;
  ConnectionForTesting con;
  NetworkForTesting net;

  @Before
  public void setup() {

    PacketHandler<Connection> handler = new SimplePacketHandler<Connection>() {
      @Override
      public void handle(Connection ctx, DataStoreResponseMessage msg) throws Exception {
        ctx.getDataStore().handle(msg);
      }

      @Override
      public void handle(Connection ctx, DataStoreRequestMessage msg) throws Exception {
        router.handle(ctx, msg);
      }

    };

    home = new HomeNode(UUID.randomUUID());
    con = new ConnectionForTesting(home, handler);
    net = new NetworkForTesting(con);
    net.setInstance();

    router = new RouterDataStore(Logger.getGlobal());
  }

  @Test
  public void add() {
    con.getDataStore().
  }

  @After
  public void teardown() {

  }


}
