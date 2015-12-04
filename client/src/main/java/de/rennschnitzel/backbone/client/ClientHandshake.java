package de.rennschnitzel.backbone.client;

import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractFuture;

import de.rennschnitzel.backbone.Protocol;
import de.rennschnitzel.backbone.api.network.Connection;
import de.rennschnitzel.backbone.api.network.ConnectionFuture;
import de.rennschnitzel.backbone.api.network.RouterInfo;
import de.rennschnitzel.backbone.net.protocol.ComponentUUID;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.AuthChallenge;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.AuthResponse;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.AuthSuccess;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.FirstLogin;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.Login;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.SecondLogin;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.Connected;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.Disconnected;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.ServerUpdate;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ErrorMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.Message;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.Packet;
import de.rennschnitzel.backbone.netty.PacketHandler;
import de.rennschnitzel.backbone.netty.PacketUtil;
import de.rennschnitzel.backbone.netty.exception.ConnectionException;
import de.rennschnitzel.backbone.netty.exception.HandshakeException;
import de.rennschnitzel.backbone.netty.exception.ProtocolException;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public abstract class ClientHandshake extends AbstractFuture<Connection>
    implements ChannelFutureListener, ConnectionFuture {

  public static enum State {
    NEW, LOGIN, AUTH, END;
  }

  @Getter
  private volatile State state = State.NEW;

  private Channel channel = null;
  @NonNull
  private final Client client;
  @NonNull
  private final RouterInfo router;

  @Getter
  private boolean firstLogin = false;

  @Getter
  private final HandshakePacketHandler handler = new HandshakePacketHandler();

  @Override
  public void cancelOrClose() {
    if (!this.cancel(true)) {
      if (channel != null) {
        channel.close();
      }
    }
  }


  public boolean isSuccess() {
    if (!this.isDone() || this.isCancelled()) {
      return false;
    }
    try {
      this.get();
      return true;
    } catch (Exception e) {
    }
    return false;
  }

  @Override
  public void operationComplete(ChannelFuture connectFuture) throws Exception {
    // Connect Future
    if (connectFuture.isSuccess()) {

    } else if (connectFuture.isCancelled()) {
      cancel(true);
    } else {
      setException(connectFuture.cause());
    }
  }

  @Override
  public synchronized boolean cancel(boolean mayInterruptIfRunning) {
    if (!super.isDone()) {
      setExceptionAndClose(new CancellationException());
    }
    this.state = State.END;
    return super.cancel(mayInterruptIfRunning);
  }

  @Override
  protected synchronized boolean set(Connection con) {
    try {
      checkState(State.AUTH);
      Preconditions.checkNotNull(con);
      this.state = State.END;
      return super.set(con);
    } catch (HandshakeException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected synchronized boolean setException(Throwable cause) {
    this.state = State.END;
    return super.setException(cause);
  }

  protected synchronized boolean setExceptionAndClose(Throwable cause) {
    if (channel != null) {
      boolean fatal = true;
      ErrorMessage.Type type;
      String text;
      if (cause instanceof ConnectionException) {
        ConnectionException con = (ConnectionException) cause;
        fatal = con.isFatal();
        type = con.getType();
        text = con.getMessage();
        if (con.isDoLog()) {
          client.getLogger().log(Level.INFO,
              type.name() + " " + con.getMessage() + (fatal ? " (fatal)" : ""), con);
        }
      } else {
        type = ErrorMessage.Type.SERVER_ERROR;
        text = "Server Error: " + cause.getMessage();
      }

      close(ErrorMessage.newBuilder().setType(type).setMessage(text));
    }
    return setException(cause);
  }

  private void close(ErrorMessage.Builder error) {
    if (channel != null) {
      if (channel.isActive()) {
        ChannelFuture f =
            PacketUtil.writeAndFlush(channel, CloseMessage.newBuilder().setError(error));
        f.addListener(ChannelFutureListener.CLOSE);
        f.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
      } else {
        channel.close();
      }
    }
  }

  private void checkState(State state) throws HandshakeException {
    if (this.state != state) {
      throw new HandshakeException(
          "Wrong state (\"" + this.state.name() + "\" should be \"" + state.name() + "\")!");
    }
  };


  private void setState(State state) throws HandshakeException {
    if (this.state != State.END && !this.isDone()) {
      this.state = state;
    } else if (this.isCancelled()) {
      throw new HandshakeException("Handshake cancelled!");
    }
  }

  protected abstract Connection upgrade(Channel channel, AuthSuccess authSuccess) throws Exception;

  // *****************************************************************************
  // PacketHandler

  private class HandshakePacketHandler extends PacketHandler {

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final Packet packet)
        throws Exception {
      try {
        super.channelRead0(ctx, packet);
      } catch (Exception e) {
        setExceptionAndClose(e);
        throw e;
      }
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
      checkState(State.NEW);
      channel = ctx.channel();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      checkState(State.NEW);
      if (client.getUuid().setSuccess(ClientHandshake.this)) {
        // First Login
        firstLogin = true;

        FirstLogin.Builder b = FirstLogin.newBuilder();
        b.addAllNamespaces(client.getNamespaces());
        b.setType(client.getType());

        PacketUtil.writeAndFlush(ctx.channel(),
            Login.newBuilder().setFirst(b).setProtocolVersion(Protocol.VERSION));

      } else {
        // Needs to be a fixed login, so it waits on result of the uuid container.

        SecondLogin.Builder b = SecondLogin.newBuilder();

        UUID id = client.getUuid().get(3, TimeUnit.SECONDS);

        b.setId(ComponentUUID.UUID.newBuilder().setMostSignificantBits(id.getMostSignificantBits())
            .setLeastSignificantBits(id.getLeastSignificantBits()));
        b.addAllNamespaces(client.getNamespaces());
        b.setType(client.getType());

        PacketUtil.writeAndFlush(ctx.channel(),
            Login.newBuilder().setSecond(b).setProtocolVersion(Protocol.VERSION));
      }

      setState(State.LOGIN);
    }

    @Override
    protected void handle(ChannelHandlerContext ctx, ErrorMessage error) throws Exception {
      ConnectionException e = null;
      switch (error.getType()) {
        case PROTOCOL_ERROR:
          e = new ProtocolException(error.getMessage());
          break;
        default:
          e = new ConnectionException(error);
      }
      setException(e);
      ctx.close();
    }

    @Override
    protected void handle(ChannelHandlerContext ctx, Login login) throws Exception {
      throw new HandshakeException("Invalid or unknown packet!");
    }

    @Override
    protected void handle(ChannelHandlerContext ctx, AuthChallenge authChallenge) throws Exception {
      checkState(State.LOGIN);
      PacketUtil.writeAndFlush(ctx.channel(),
          AuthResponse.newBuilder().setToken(authChallenge.getToken()));
      setState(State.AUTH);
    }

    @Override
    protected void handle(ChannelHandlerContext ctx, AuthResponse authResponse) throws Exception {
      throw new HandshakeException("Invalid or unknown packet!");
    }

    @Override
    protected void handle(ChannelHandlerContext ctx, AuthSuccess authSuccess) throws Exception {
      checkState(State.AUTH);
      ClientHandshake.this.set(ClientHandshake.this.upgrade(ctx.channel(), authSuccess));
    }

    @Override
    protected void handle(ChannelHandlerContext ctx, Connected connected) throws Exception {
      throw new HandshakeException("Invalid or unknown packet!");
    }

    @Override
    protected void handle(ChannelHandlerContext ctx, Disconnected disconnected) throws Exception {
      throw new HandshakeException("Invalid or unknown packet!");
    }

    @Override
    protected void handle(ChannelHandlerContext ctx, Message message) throws Exception {
      throw new HandshakeException("Invalid or unknown packet!");
    }

    @Override
    protected void handle(ChannelHandlerContext ctx, ServerUpdate update) throws Exception {
      throw new HandshakeException("Invalid or unknown packet!");
    }
  }



}
