package de.rennschnitzel.backbone.router;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.ByteString;

import de.rennschnitzel.backbone.api.network.Connection;
import de.rennschnitzel.backbone.api.network.RouterInfo;
import de.rennschnitzel.backbone.net.protocol.ComponentUUID;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.AuthChallenge;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.AuthResponse;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.AuthSuccess;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.FirstLogin;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.Login;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.SecondLogin;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.Server;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ErrorMessage;
import de.rennschnitzel.backbone.netty.HandshakeHandler;
import de.rennschnitzel.backbone.netty.PacketUtil;
import de.rennschnitzel.backbone.netty.exception.ConnectionException;
import de.rennschnitzel.backbone.netty.exception.HandshakeException;
import de.rennschnitzel.backbone.netty.exception.ProtocolException;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;

public class RouterHandshakeHandler extends HandshakeHandler {

  private static final SecureRandom random = new SecureRandom();

  public static synchronized byte[] nextChallenge(int length) {
    byte[] result = new byte[length];
    random.nextBytes(result);
    return result;
  }

  @Getter
  private boolean firstLoginHandler = false;

  // private final SettableFuture<AuthSuccess> success = SettableFuture.create();
  private final SettableFuture<ClientConnection> connection = SettableFuture.create();
  @Getter
  private final Router router;
  @Getter
  private Server client;

  private Login login = null;
  private final ByteString challenge = ByteString.copyFrom(nextChallenge(32));

  public RouterHandshakeHandler(Router router) {
    Preconditions.checkNotNull(router);
    this.router = router;

    // Cancel propagation
    Futures.addCallback(connection, new FutureCallback<ClientConnection>() {
      @Override
      public void onFailure(Throwable cause) {
        if (getChannelContext() != null) {
          getChannelContext().close();
        }
      }

      @Override
      public void onSuccess(ClientConnection value) {}
    });

  }

  @Override
  protected void onFail(Throwable cause) {
    this.connection.setException(cause);
  }

  public ListenableFuture<ClientConnection> getConnectionFuture() {
    return this.connection;
  }


  private void checkFutures() throws HandshakeException {
    if (this.connection.isDone()) {
      boolean cancelled = this.connection.isCancelled();
      throw new HandshakeException(cancelled ? "cancelled" : "already done");
    }
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    checkFutures();
    this.setState(State.LOGIN);
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
    this.fail(e);
  }


  @Override
  protected void handle(ChannelHandlerContext ctx, Login login) throws HandshakeException {
    this.checkState(State.LOGIN);

    switch (login.getLoginCase()) {
      case FIRST:
        break;
      case SECOND:
        if (login.getSecond().getId() == null) {
          throw new HandshakeException("Server ID required!");
        }
        break;
      default:
        throw new HandshakeException("Invalid or unknown packet!");
    }

    this.login = login;
    PacketUtil.writeAndFlush(ctx, AuthChallenge.newBuilder().setToken(challenge));
    this.setState(State.AUTH);
  }

  @Override
  protected void handle(ChannelHandlerContext ctx, AuthResponse authResponse)
      throws HandshakeException {
    this.checkState(State.AUTH);

    if (!this.challenge.equals(authResponse.getToken())) {
      throw new HandshakeException("Invalid authentification!");
      return;
    }

    AuthSuccess.Builder b = AuthSuccess.newBuilder();
    b.setRouter(router.toProtocol());
    b.setClient(value)
    PacketUtil.writeAndFlush(ctx, b);
    this.setState(State.SUCCESS);    
  }


  @Override
  protected void handle(ChannelHandlerContext ctx, AuthChallenge authChallenge) throws Exception {
    throw new HandshakeException("Invalid or unknown packet!");
  }


  @Override
  protected void handle(ChannelHandlerContext ctx, AuthSuccess authSuccess) throws Exception {
    throw new HandshakeException("Invalid or unknown packet!");
  }

  protected abstract RouterConnection upgrade(ChannelHandlerContext ctx, AuthSuccess authSuccess);


}
