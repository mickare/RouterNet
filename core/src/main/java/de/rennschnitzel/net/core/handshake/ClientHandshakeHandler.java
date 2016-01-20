package de.rennschnitzel.net.core.handshake;

import java.io.IOException;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;

import de.rennschnitzel.net.Protocol;
import de.rennschnitzel.net.ProtocolUtils;
import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.exception.ConnectionException;
import de.rennschnitzel.net.exception.ProtocolException;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginChallengeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginHandshakeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginResponseMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginSuccessMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginUpgradeMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ErrorMessage;
import lombok.Getter;

public abstract class ClientHandshakeHandler<C extends Connection, I> extends AbstractHandshakeHandler<C, I> {

  @Getter
  private final AbstractNetwork network;
  private final ClientAuthentication authentication;

  public ClientHandshakeHandler(String handlerName, AbstractNetwork network, ClientAuthentication authentication) {
    super(handlerName);
    Preconditions.checkNotNull(network);
    Preconditions.checkNotNull(authentication);
    this.network = network;
    this.authentication = authentication;
  }

  @Override
  public void handle(I ctx, CloseMessage msg) throws Exception {
    switch (msg.getReasonCase()) {
      case ERROR:
        this.fail(new ConnectionException(msg.getError()));
        break;
      case NORMAL:
        this.fail(new ConnectionException(ErrorMessage.Type.UNRECOGNIZED, msg.getNormal()));
        break;
      case SHUTDOWN:
        this.fail(new ConnectionException(ErrorMessage.Type.UNAVAILABLE, "shutdown"));
        break;
      default:
        fail(new ProtocolException("invalid close reason"));
    }
  }

  public void contextActive(I ctx) throws Exception {
    checkAndSetState(State.NEW, State.LOGIN);
    LoginHandshakeMessage.Builder msg = LoginHandshakeMessage.newBuilder();
    msg.setProtocolVersion(Protocol.VERSION);
    msg.setId(ProtocolUtils.convert(network.getHome().getId()));
    Optional<String> name = network.getHome().getName();
    if (name.isPresent()) {
      msg.setName(name.get());
    }
    send(ctx, msg.build());
  }

  protected abstract void send(I ctx, LoginHandshakeMessage handshake) throws Exception;

  @Override
  public void handle(I ctx, LoginHandshakeMessage msg) throws Exception {
    throw new ProtocolException("invalid packet");
  }

  @Override
  public void handle(I ctx, LoginResponseMessage msg) throws Exception {
    throw new ProtocolException("invalid packet");
  }

  @Override
  public void handle(I ctx, LoginChallengeMessage msg) throws Exception {
    checkAndSetState(State.LOGIN, State.AUTH);
    ByteString response = this.authentication.calculateResponse(msg.getToken());
    send(ctx, LoginResponseMessage.newBuilder().setToken(response).build());
  }

  protected abstract void send(I ctx, LoginResponseMessage response) throws Exception;

  @Override
  public void handle(I ctx, LoginSuccessMessage msg) throws Exception {
    checkState(State.AUTH);
    send(ctx, upgrade(ctx, msg));
  }

  protected abstract LoginUpgradeMessage upgrade(I ctx, LoginSuccessMessage msg) throws Exception;

  protected abstract void send(I ctx, LoginUpgradeMessage upgrade) throws IOException;

  @Override
  public void handle(I ctx, LoginUpgradeMessage msg) throws Exception {
    throw new ProtocolException("invalid packet");
  }

  @Override
  protected void onFail(Throwable cause) {
    if (!this.isActive()) {
      return;
    }
    I ctx = this.getContext();
    if (ctx != null) {
      try {
        send(ctx, CloseMessage.newBuilder()
            .setError(ErrorMessage.newBuilder().setType(ErrorMessage.Type.HANDSHAKE).setMessage(cause.getMessage())).build());
      } catch (IOException e) {
        // closing anyways...
      }
    }
  }

  protected abstract void send(I ctx, CloseMessage msg) throws IOException;

}
