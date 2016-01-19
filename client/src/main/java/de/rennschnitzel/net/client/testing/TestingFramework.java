package de.rennschnitzel.net.client.testing;

import java.util.UUID;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.NetClient;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.Node.HomeNode;
import de.rennschnitzel.net.core.packet.BasePacketHandler;
import de.rennschnitzel.net.dummy.DummyConnection;
import de.rennschnitzel.net.dummy.DummyNetwork;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import lombok.Getter;

public class TestingFramework {

  public static enum Mode {
    NONE, DUMMY, NETTY;
  }

  @Getter
  private final NetClient client;

  @Getter
  private final DummyNetwork network;

  @Getter
  private final Mode mode;

  public TestingFramework(NetClient client, Mode mode) {
    Preconditions.checkNotNull(client);
    Preconditions.checkNotNull(mode);
    Preconditions.checkArgument(mode != Mode.NONE);
    this.client = client;
    this.mode = mode;
    this.network = new DummyNetwork(client.getExecutor(), new HomeNode(UUID.randomUUID()));
    this.network.setConnection(new DummyConnection(network, new BasePacketHandler<>()));
  }

  protected TestingConnector<? extends Connection> connect() {
    if (mode == Mode.DUMMY) {
      Preconditions.checkState(network.getConnection() instanceof DummyConnection);
      DummyConnection con = (DummyConnection) this.network.getConnection();
      DummyConnection result = new DummyConnection(network, new BasePacketHandler<>());
      con.connect(result);
      return new DummyConnector(result, con);
    } else if (mode == Mode.NETTY) {

    }
    throw new IllegalStateException("no connection for mode " + mode);
  }


  protected void disconnect(TestingConnector con, CloseMessage msg) {
    con.disconnect(msg);
  }

  



}
