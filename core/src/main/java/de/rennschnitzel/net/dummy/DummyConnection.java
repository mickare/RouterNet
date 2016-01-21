package de.rennschnitzel.net.dummy;

import java.io.IOException;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.packet.PacketHandler;
import de.rennschnitzel.net.event.ConnectionEvent;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class DummyConnection extends Connection {

  private static final Object lockObj = new Object();


  @Getter
  private boolean closed = false;

  @Setter
  @NonNull
  private PacketHandler<DummyConnection> handler;

  @Getter
  @NonNull
  private DummyConnection connected = null;


  public DummyConnection(AbstractNetwork network, PacketHandler<DummyConnection> handler) {
    super(network);
    Preconditions.checkNotNull(handler);
    this.handler = handler;
    try {
      handler.handlerAdded(this);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void connect(DummyConnection connection) {
    Preconditions.checkNotNull(connection);
    Preconditions.checkArgument(connection != this);
    synchronized (lockObj) {
      Preconditions.checkState(this.connected == null);
      Preconditions.checkState(!this.closed, "closed");
      this.connected = connection;
      connection.connected = this;

      this.setId(connection.getNetwork().getHome().getId());
      this.connected.setId(this.getNetwork().getHome().getId());

      try {
        this.connected.handler.contextActive(this.connected);
        this.handler.contextActive(this);
      } catch (Exception e) {
        throw new RuntimeException(e.getMessage(), e);
      }

      this.getNetwork().getHome().publishChanges();
      connection.getNetwork().getHome().publishChanges();


      this.getNetwork().getEventBus().post(new ConnectionEvent.OpenConnectionEvent(this));
      connection.getNetwork().getEventBus().post(new ConnectionEvent.OpenConnectionEvent(connection));

    }
  }

  public boolean isActive() {
    return !this.isClosed() && this.connected != null;
  }

  public void disconnect() {
    disconnect(CloseMessage.newBuilder().setNormal("normal disconnect").build());
  }

  public void disconnect(CloseMessage msg) {
    Preconditions.checkNotNull(msg);
    try {
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
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void receive(Packet packet) throws Exception {
    if (closed) {
      return;
    }
    handler.handle(this, packet);
  }

  @Override
  public void send(Packet packet) throws IOException {
    if (closed) {
      return;
    }
    try {
      connected.receive(packet);
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

}
