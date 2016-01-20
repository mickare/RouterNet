package de.rennschnitzel.net.core.handshake;

import java.io.IOException;
import java.util.UUID;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.Protocol;
import de.rennschnitzel.net.ProtocolUtils;
import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.exception.ConnectionException;
import de.rennschnitzel.net.exception.HandshakeException;
import de.rennschnitzel.net.exception.ProtocolException;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginChallengeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginHandshakeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginResponseMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginSuccessMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginUpgradeMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ErrorMessage;
import lombok.Getter;

public abstract class RouterHandshakeHandler<C extends Connection, I> extends AbstractHandshakeHandler<C, I> {

  @Getter
  private final AbstractNetwork network;
  private final RouterAuthentication authentication;

  @Getter
  private int protocolVersion = -1;
  @Getter
  private UUID clientId = null;
  @Getter
  private String name = null;

  public RouterHandshakeHandler(String handlerName, AbstractNetwork network, RouterAuthentication authentication) {
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
    checkState(State.NEW);
  }

  @Override
  public void handle(I ctx, LoginHandshakeMessage msg) throws Exception {
    checkAndSetState(State.NEW, State.LOGIN);

    if (msg.getProtocolVersion() > Protocol.VERSION) {
      throw new ProtocolException("Router has outdated protocol version");
    } else if (msg.getProtocolVersion() < Protocol.VERSION) {
      throw new ProtocolException("Client has outdated protocol version");
    }

    this.protocolVersion = msg.getProtocolVersion();
    this.clientId = ProtocolUtils.convert(msg.getId());
    this.name = msg.getName();

    send(ctx, LoginChallengeMessage.newBuilder().setToken(this.authentication.getChallenge()).build());
  }

  protected abstract void send(I ctx, LoginChallengeMessage msg) throws Exception;

  @Override
  public void handle(I ctx, LoginResponseMessage msg) throws Exception {
    checkAndSetState(State.LOGIN, State.AUTH);

    if (!this.authentication.checkResponse(msg.getToken())) {
      throw new HandshakeException("invalid login");
    }

    LoginSuccessMessage.Builder b = LoginSuccessMessage.newBuilder();
    b.setRouterId(ProtocolUtils.convert(this.network.getHome().getId()));
    b.setTopology(this.network.getTopologyMessage());
    send(ctx, b.build());

  }

  protected abstract void send(I ctx, LoginSuccessMessage msg) throws Exception;

  @Override
  public void handle(I ctx, LoginUpgradeMessage msg) throws Exception {
    checkState(State.AUTH);
    upgrade(ctx, msg);
  }

  protected abstract void upgrade(I ctx, LoginUpgradeMessage msg) throws HandshakeException;

  @Override
  public void handle(I ctx, LoginChallengeMessage msg) throws Exception {
    throw new ProtocolException("invalid packet");
  }

  @Override
  public void handle(I ctx, LoginSuccessMessage msg) throws Exception {
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
        // closing anyways
      }
    }
  }

  protected abstract void send(I ctx, CloseMessage msg) throws IOException;

}
