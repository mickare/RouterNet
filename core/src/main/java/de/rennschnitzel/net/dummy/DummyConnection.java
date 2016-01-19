package de.rennschnitzel.net.dummy;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.packet.PacketHandler;
import de.rennschnitzel.net.event.ConnectionEvent;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import lombok.Getter;
import lombok.NonNull;

public class DummyConnection extends Connection {

  private static final Object lockObj = new Object();


  @Getter
  private boolean closed = false;

  private final PacketHandler<DummyConnection> handler;

  @Getter
  @NonNull
  private DummyConnection connected = null;


  public DummyConnection(AbstractNetwork network, PacketHandler<DummyConnection> handler) {
    super(network);
    Preconditions.checkNotNull(handler);
    this.handler = handler;
  }

  public void connect(DummyConnection connection) {
    Preconditions.checkNotNull(connection);
    Preconditions.checkArgument(connection != this);
    synchronized (lockObj) {
      Preconditions.checkState(this.connected == null);
      Preconditions.checkState(!this.closed, "closed");
      this.connected = connection;
      connection.connected = this;

      this.getNetwork().getHome().publishChanges();
      connection.getNetwork().getHome().publishChanges();


      this.getNetwork().getEventBus().post(new ConnectionEvent.OpenConnectionEvent(this));
      connection.getNetwork().getEventBus().post(new ConnectionEvent.OpenConnectionEvent(connection));

    }
  }


  public void disconnect() {
    disconnect(CloseMessage.newBuilder().setNormal("normal disconnect").build());
  }

  public void disconnect(CloseMessage msg) {
    Preconditions.checkNotNull(msg);
    synchronized (lockObj) {
      if (this.closed) {
        return;
      }
      this.closed = true;
      if (this.connected != null) {
        this.setCloseMessage(msg);
        this.connected.receive(Packet.newBuilder().setClose(msg).build());
        this.connected.closed = true;
        this.connected.connected = null;
        this.connected.getNetwork().getEventBus().post(new ConnectionEvent.ClosedConnectionEvent(this.connected));
      }
      this.connected = null;
      this.getNetwork().getEventBus().post(new ConnectionEvent.ClosedConnectionEvent(this));
    }
  }

  public void receive(Packet packet) {
    if (closed) {
      return;
    }
    try {
      handler.handle(this, packet);
    } catch (Exception pe) {
      throw new RuntimeException(pe);
    }
  }

  @Override
  public void send(Packet packet) {
    if (closed) {
      return;
    }
    connected.receive(packet);
  }

}
