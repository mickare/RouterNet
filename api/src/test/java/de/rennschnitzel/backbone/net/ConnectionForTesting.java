package de.rennschnitzel.backbone.net;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.net.packet.PacketHandler;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.Packet;
import lombok.Getter;
import lombok.NonNull;

public class ConnectionForTesting extends Connection {

  private static final Object lockObj = new Object();


  @Getter
  private boolean closed = false;

  private final PacketHandler<Connection> handler;

  @Getter
  @NonNull
  private ConnectionForTesting connected = null;


  public ConnectionForTesting(Network network, PacketHandler<Connection> handler) {
    super(network);
    Preconditions.checkNotNull(handler);
    this.handler = handler;
  }

  public void connect(ConnectionForTesting connection) {
    synchronized (lockObj) {
      this.connected = connection;
      connection.connected = this;
    }
  }

  public void disconnect() {
    this.closed = true;
    synchronized (lockObj) {
      if (this.connected != null && this.connected.connected == this) {
        this.connected.connected = null;
      }
      this.connected = null;
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
