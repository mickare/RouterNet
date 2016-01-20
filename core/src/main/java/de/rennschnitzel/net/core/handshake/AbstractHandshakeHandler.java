package de.rennschnitzel.net.core.handshake;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.SettableFuture;

import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.exception.ConnectionException;
import de.rennschnitzel.net.exception.HandshakeException;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeRemoveMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeTopologyMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeUpdateMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ChannelMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ChannelRegister;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import de.rennschnitzel.net.core.ConnectionFuture;
import de.rennschnitzel.net.core.packet.PacketHandler;

public abstract class AbstractHandshakeHandler<C extends Connection, I> implements PacketHandler<I>, ConnectionFuture<C> {

  @RequiredArgsConstructor
  public static enum State {
    NEW(0), LOGIN(1), AUTH(1), SUCCESS(3), FAILED(3);
    @Getter
    private final int step;
  }


  public static final int VERSION = 1;

  @Getter
  private final String name;
  @Getter
  private volatile State state = State.NEW;

  private final SettableFuture<C> delegate = SettableFuture.create();

  @Getter
  private I context = null;

  public AbstractHandshakeHandler(String name) {
    Preconditions.checkArgument(!name.isEmpty());
    this.name = name;
  }

  public abstract boolean isOpen();

  public abstract boolean isActive();

  protected abstract void onFail(Throwable cause);

  @Override
  public void handlerAdded(I ctx) throws Exception {
    this.checkState(State.NEW);
    Preconditions.checkState(this.context == null);
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

  protected synchronized final void setSuccess(final C connection) throws HandshakeException {
    if (this.state.step >= State.SUCCESS.step) {
      throw new HandshakeException("Handshake already completed with " + this.state);
    }
    Preconditions.checkNotNull(connection);
    if (!this.delegate.set(connection)) {
      throw new HandshakeException("handshake already set or cancelled");
    }
    this.state = State.SUCCESS;
  }


  public synchronized final void fail(Throwable cause) {
    if (this.state.step >= State.FAILED.step) {
      return;
    }
    this.state = State.FAILED;
    this.delegate.setException(cause);
    onFail(cause);
  }

  protected synchronized final void checkState(final State setpoint) throws ConnectionException {
    if (this.state != setpoint) {
      throw new HandshakeException("Wrong state (\"" + this.state.name() + "\" should be \"" + setpoint.name() + "\")!");
    }
  }

  protected synchronized void checkAndSetState(final State expected, final State newState) throws ConnectionException {
    this.checkState(expected);
    this.setState(newState);
  }

  public void handle(I ctx, Packet packet) throws Exception {
    try {
      PacketHandler.super.handle(ctx, packet);
    } catch (final Exception e) {
      if (this.state != State.FAILED && this.state != State.SUCCESS) {
        this.fail(e);
      }
      throw e;
    }
  }

  /*
   * @Override public void exceptionCaught(I ctx, Throwable cause) throws Exception { if (this.state
   * != State.FAILED && this.state != State.SUCCESS) { this.fail(cause); }
   * super.exceptionCaught(ctx, cause); }
   * 
   * @Override protected void channelRead0(final I ctx, final Packet packet) throws Exception { try
   * { super.channelRead0(ctx, packet); } catch (final Exception e) { if (this.state != State.FAILED
   * && this.state != State.SUCCESS) { this.fail(e); } throw e; } }
   */

  @Override
  public void handle(I ctx, NodeUpdateMessage msg) throws HandshakeException {
    throw new HandshakeException("Invalid or unknown packet!");
  }

  @Override
  public void handle(I ctx, NodeRemoveMessage msg) throws HandshakeException {
    throw new HandshakeException("Invalid or unknown packet!");
  }

  @Override
  public void handle(I ctx, NodeTopologyMessage msg) throws HandshakeException {
    throw new HandshakeException("Invalid or unknown packet!");
  }

  @Override
  public void handle(I ctx, ChannelMessage message) throws HandshakeException {
    throw new HandshakeException("Invalid or unknown packet!");
  }


  @Override
  public void handle(I ctx, ProcedureMessage msg) throws HandshakeException {
    throw new HandshakeException("Invalid or unknown packet!");
  }

  @Override
  public void handle(I ctx, ChannelRegister msg) throws HandshakeException {
    throw new HandshakeException("Invalid or unknown packet!");
  }


  // Future stuff

  @Override
  public synchronized boolean cancel(boolean mayInterruptIfRunning) {
    if (delegate.cancel(mayInterruptIfRunning)) {
      this.fail(new CancellationException());
      return true;
    }
    return false;
  }

  @Override
  public boolean isCancelled() {
    return delegate.isCancelled();
  }

  @Override
  public boolean isDone() {
    return delegate.isDone();
  }

  @Override
  public C get() throws InterruptedException, ExecutionException {
    return delegate.get();
  }

  @Override
  public C get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return delegate.get(timeout, unit);
  }

  @Override
  public void addListener(Runnable listener, Executor exec) {
    delegate.addListener(listener, exec);
  }
}
