package de.rennschnitzel.backbone.netty;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.exception.ConnectionException;
import de.rennschnitzel.backbone.exception.HandshakeException;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.ConnectedMessage;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.DisconnectedMessage;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.ServerUpdateMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ChannelMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ChannelRegister;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.Packet;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureMessage;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public abstract class HandshakeHandler extends NettyPacketHandler {

  public static final int VERSION = 1;

  
  @RequiredArgsConstructor
  public static enum State {
    NEW(0), LOGIN(1), AUTH(1), SUCCESS(3), FAILED(3);
    @Getter
    private final int step;
  }
  
  private volatile State state = State.NEW;
  @Getter
  private ChannelHandlerContext channelContext = null;

  protected abstract void onFail(Throwable cause);
  
  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    this.checkState(State.NEW);
    Preconditions.checkState(this.channelContext == null);
    this.channelContext = ctx;
  }

  public final State state() {
    return this.state;
  }

  protected synchronized final void setState(final State state) throws HandshakeException {
    Preconditions.checkArgument(state != State.FAILED);
    if (this.state.step < state.step) {
      throw new HandshakeException("Can only increase state step!");
    }
    this.state = state;
  }



  public synchronized final void fail(Throwable cause) {
    if (this.state.step < State.FAILED.step) {
      return;
    }
    this.state = State.FAILED;
    onFail(cause);
  }

  protected synchronized final void checkState(final State setpoint) throws ConnectionException {
    if (this.state != setpoint) {
      throw new HandshakeException("Wrong state (\"" + this.state.name() + "\" should be \"" + setpoint.name() + "\")!");
    }
  }

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx, final Packet packet) throws Exception {
    try {
      super.channelRead0(ctx, packet);
    } catch (final Exception e) {
      if (this.state != State.FAILED && this.state != State.SUCCESS) {
        this.fail(e);
      }
      throw e;
    }
  }

  @Override
  public void handle(ChannelHandlerContext ctx, ConnectedMessage connected) throws HandshakeException {
    throw new HandshakeException("Invalid or unknown packet!");
  }

  @Override
  public void handle(ChannelHandlerContext ctx, DisconnectedMessage disconnected) throws HandshakeException {
    throw new HandshakeException("Invalid or unknown packet!");
  }

  @Override
  public void handle(ChannelHandlerContext ctx, ChannelMessage message) throws HandshakeException {
    throw new HandshakeException("Invalid or unknown packet!");
  }


  @Override
  public void handle(ChannelHandlerContext ctx, ProcedureMessage msg) throws HandshakeException {
    throw new HandshakeException("Invalid or unknown packet!");
  }

  @Override
  public void handle(ChannelHandlerContext ctx, ChannelRegister msg) throws HandshakeException {
    throw new HandshakeException("Invalid or unknown packet!");
  }

  @Override
  public void handle(ChannelHandlerContext ctx, ServerUpdateMessage msg) throws HandshakeException {
    throw new HandshakeException("Invalid or unknown packet!");
  }


}
