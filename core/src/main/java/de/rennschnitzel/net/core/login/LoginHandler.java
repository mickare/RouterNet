package de.rennschnitzel.net.core.login;

import java.util.UUID;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.ConnectionSupplier;
import de.rennschnitzel.net.core.packet.PacketHandler;
import de.rennschnitzel.net.exception.ConnectionException;
import de.rennschnitzel.net.exception.HandshakeException;
import de.rennschnitzel.net.exception.ProtocolException;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeRemoveMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeTopologyMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeUpdateMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ErrorMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.HeartbeatMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.TunnelMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.TunnelRegister;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

public abstract class LoginHandler<C> implements PacketHandler<C>, ConnectionSupplier {


  @Getter
  private final AbstractNetwork network;

  @RequiredArgsConstructor
  public static enum State {
    NEW(0), LOGIN(1), AUTH(1), SUCCESS(3), FAILED(3);
    @Getter
    private final int step;
  }

  @Getter
  private final String handlerName;
  @Getter
  private volatile State state = State.NEW;

  @Getter
  @Setter(AccessLevel.PACKAGE)
  private C context = null;

  @Getter
  private Throwable failureCause = null;
  @Getter
  private State failureState = null;

  @Getter
  private Promise<Connection> connectionPromise = ImmediateEventExecutor.INSTANCE.newPromise();

  public LoginHandler(String handlerName, AbstractNetwork network) {
    Preconditions.checkArgument(!handlerName.isEmpty());
    Preconditions.checkNotNull(network);
    this.handlerName = handlerName;
    this.network = network;

    this.connectionPromise.addListener(f -> {
      if (!f.isSuccess()) {
        LoginHandler.this.fail(f.cause());
      }
    });
  }

  @Override
  public void channelInactive(C ctx) throws Exception {
    if (!this.isDone()) {
      this.fail(ctx, new IllegalStateException("channel inactive"));
    }
  }

  protected void addFailListener(final C ctx, final Future<?> future) {
    future.addListener(f -> {
      if (!f.isSuccess()) {
        this.fail(ctx, f.cause());
      }
    });
  }

  public abstract UUID getId();

  public abstract String getName();

  public abstract void registerNodes();

  public boolean isSuccess() {
    return this.state == State.SUCCESS;
  }

  public boolean isDone() {
    State s = this.state;
    return s == State.SUCCESS || s == State.FAILED;
  }

  @Override
  public void handlerAdded(C ctx) throws Exception {
    this.checkState(State.NEW);
    this.context = ctx;
  }

  protected synchronized final void setState(final State state) throws HandshakeException {
    Preconditions.checkArgument(state != State.FAILED);
    Preconditions.checkArgument(state != State.SUCCESS);
    if (state.step < this.state.step) {
      throw new HandshakeException("Can only increase state step!");
    }
    this.state = state;
  }

  protected synchronized final void setSuccess() throws HandshakeException {
    if (this.state.step >= State.SUCCESS.step) {
      throw new HandshakeException("Handshake already completed with " + this.state);
    }
    this.state = State.SUCCESS;
  }


  public final boolean fail(Throwable cause) {
    return this.fail(this.context, cause);
  }

  public final boolean fail(C ctx, Throwable cause) {
    synchronized (this) {
      if (this.state.step >= State.FAILED.step) {
        return false;
      }
      this.failureCause = cause;
      this.failureState = this.state;
      this.state = State.FAILED;
    }
    onFail(ctx, cause);
    return true;
  }

  protected void onFail(C ctx, Throwable cause) {
    this.connectionPromise.tryFailure(cause);
    this.getNetwork().getLogger().info(this.getName() + " (" + this.getId() + ") failed handshake: " + cause.getMessage());
    if (ctx != null) {
      send(ctx, CloseMessage.newBuilder()
          .setError(ErrorMessage.newBuilder().setType(ErrorMessage.Type.HANDSHAKE).setMessage(cause.getMessage())).build());
    }
  }

  protected abstract Future<?> send(C ctx, CloseMessage msg);

  protected synchronized final void checkState(final State setpoint) throws ConnectionException {
    if (this.state != setpoint) {
      throw new HandshakeException("Wrong state (\"" + this.state.name() + "\" should be \"" + setpoint.name() + "\")!");
    }
  }

  protected synchronized void checkAndSetState(final State expected, final State newState) throws ConnectionException {
    this.checkState(expected);
    this.setState(newState);
  }

  public void handle(C ctx, Packet packet) throws Exception {
    try {
      PacketHandler.super.handle(ctx, packet);
    } catch (final Exception e) {
      this.fail(ctx, e);
      throw e;
    }
  }



  @Override
  public void handle(C ctx, HeartbeatMessage heartbeat) throws Exception {
    throw new HandshakeException("Invalid or unknown packet!");
  }

  @Override
  public void handle(C ctx, NodeUpdateMessage msg) throws HandshakeException {
    throw new HandshakeException("Invalid or unknown packet!");
  }

  @Override
  public void handle(C ctx, NodeRemoveMessage msg) throws HandshakeException {
    throw new HandshakeException("Invalid or unknown packet!");
  }

  @Override
  public void handle(C ctx, NodeTopologyMessage msg) throws HandshakeException {
    throw new HandshakeException("Invalid or unknown packet!");
  }

  @Override
  public void handle(C ctx, TunnelMessage message) throws HandshakeException {
    throw new HandshakeException("Invalid or unknown packet!");
  }


  @Override
  public void handle(C ctx, ProcedureMessage msg) throws HandshakeException {
    throw new HandshakeException("Invalid or unknown packet!");
  }

  @Override
  public void handle(C ctx, TunnelRegister msg) throws HandshakeException {
    throw new HandshakeException("Invalid or unknown packet!");
  }

  @Override
  public void handle(C ctx, CloseMessage msg) throws Exception {
    switch (msg.getReasonCase()) {
      case ERROR:
        this.fail(ctx, ConnectionException.of(msg.getError()));
        break;
      case NORMAL:
        this.fail(ctx, new ConnectionException(ErrorMessage.Type.UNRECOGNIZED, msg.getNormal()));
        break;
      case SHUTDOWN:
        this.fail(ctx, new ConnectionException(ErrorMessage.Type.UNAVAILABLE, "shutdown"));
        break;
      default:
        this.fail(ctx, new ProtocolException("invalid close reason"));
    }
  }

}
