package de.rennschnitzel.net.dummy;

import java.io.IOException;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.packet.PacketHandler;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import de.rennschnitzel.net.util.FutureUtils;
import io.netty.util.concurrent.Future;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class DummyConnection extends Connection {

  private static final Object lockObj = new Object();


  @Getter
  private boolean valid = true;

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
      Preconditions.checkState(this.valid, "not valid");

      this.connected = connection;
      connection.connected = this;

      this.setId(connection.getNetwork().getHome().getId());
      this.connected.setId(this.getNetwork().getHome().getId());

      try {

        // activate handlers
        this.connected.handler.channelActive(this.connected);
        this.handler.channelActive(this);

        this.getNetwork().addConnection(this);
        this.connected.getNetwork().addConnection(this.connected);

      } catch (Exception e) {
        throw new RuntimeException(e.getMessage(), e);
      }

    }
  }

  public boolean isOpen() {
    return this.valid;
  }

  public boolean isActive() {
    return this.isOpen() && this.connected != null;
  }

  public Future<?> disconnect(CloseMessage msg) {
    Preconditions.checkNotNull(msg);
    try {
      synchronized (lockObj) {
        if (!this.valid) {
          return FutureUtils.SUCCESS;
        }
        this.valid = false;
        if (this.connected != null) {
          this.setCloseMessage(msg);
          this.connected.receive(Packet.newBuilder().setClose(msg).build());

          this.connected.valid = true;
          this.connected.connected = null;
          this.connected.getNetwork().removeConnection(this.connected);
        }
        this.connected = null;
        this.getNetwork().removeConnection(this);
      }
      return FutureUtils.SUCCESS;
    } catch (Exception e) {
      return FutureUtils.futureFailure(e);
    }
  }

  public void receive(Packet packet) throws Exception {
    if (!valid) {
      return;
    }
    handler.handle(this, packet);
  }

  @Override
  public Future<?> send(Packet packet) {
    if (!valid) {
      return FutureUtils.futureFailure(new IllegalStateException("not valid connection"));
    }
    try {
      connected.receive(packet);
      return FutureUtils.SUCCESS;
    } catch (IOException e) {
      return FutureUtils.futureFailure(e);
    } catch (Exception e) {
      return FutureUtils.futureFailure(new IOException(e));
    }
  }

}
