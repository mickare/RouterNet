package de.rennschnitzel.backbone.router.netty;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.ByteString;

import de.rennschnitzel.backbone.exception.ConnectionException;
import de.rennschnitzel.backbone.exception.HandshakeException;
import de.rennschnitzel.backbone.exception.ProtocolException;
import de.rennschnitzel.backbone.net.protocol.ComponentsProtocol.UUIDMessage;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.AuthChallengeMessage;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.AuthResponseMessage;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.AuthSuccessMessage;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.LoginMessage;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.ServerMessage;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.ServerUpdateMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ChannelRegister;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ErrorMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureMessage;
import de.rennschnitzel.backbone.netty.HandshakeHandler;
import de.rennschnitzel.backbone.netty.PacketUtil;
import de.rennschnitzel.backbone.router.Router;
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
  private ServerMessage client;

  private LoginMessage login = null;
  private final ByteString challenge = ByteString.copyFrom(nextChallenge(32));

  public RouterHandshakeHandler(Router router) {
    super("RouterHandshakeHandler");
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
  public void handle(ChannelHandlerContext ctx, ErrorMessage error) throws Exception {
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
  public void handle(ChannelHandlerContext ctx, LoginMessage login) throws ConnectionException {
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
    PacketUtil.writeAndFlush(ctx.channel(), AuthChallengeMessage.newBuilder().setToken(challenge));
    this.setState(State.AUTH);
  }

  @Override
  public void handle(ChannelHandlerContext ctx, AuthResponseMessage authResponse)
      throws HandshakeException {
    this.checkState(State.AUTH);

    if (!this.challenge.equals(authResponse.getToken())) {
      throw new HandshakeException("Invalid authentification!");
      return;
    }

    AuthSuccessMessage.Builder b = AuthSuccessMessage.newBuilder();
    b.setRouter(router.toProtocol());
    b.setClient(value)
    PacketUtil.writeAndFlush(ctx, b);
    this.setState(State.SUCCESS);    
  }


  @Override
  public void handle(ChannelHandlerContext ctx, AuthChallengeMessage authChallenge)
      throws Exception {
    throw new HandshakeException("Invalid or unknown packet!");
  }


  @Override
  public void handle(ChannelHandlerContext ctx, AuthSuccessMessage authSuccess) throws Exception {
    throw new HandshakeException("Invalid or unknown packet!");
  }

  protected NettyConnection upgrade(ChannelHandlerContext ctx, AuthSuccessMessage authSuccess) {

  }

  @Override
  public void handle(ChannelHandlerContext ctx, CloseMessage msg) throws Exception {
    // TODO Auto-generated method stub

  }


}
