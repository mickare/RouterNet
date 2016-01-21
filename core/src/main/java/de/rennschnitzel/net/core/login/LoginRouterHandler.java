package de.rennschnitzel.net.core.login;

import java.util.UUID;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.Protocol;
import de.rennschnitzel.net.ProtocolUtils;
import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.exception.HandshakeException;
import de.rennschnitzel.net.exception.ProtocolException;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginChallengeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginHandshakeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginResponseMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginSuccessMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginUpgradeMessage;
import lombok.Getter;

public abstract class LoginRouterHandler<C> extends LoginHandler<C> {

  @Getter
  private final AbstractNetwork network;
  private final AuthenticationRouter authentication;

  @Getter
  private int protocolVersion = -1;
  @Getter
  private UUID id = null;
  @Getter
  private String name = null;

  public LoginRouterHandler(String handlerName, AbstractNetwork network, AuthenticationRouter authentication) {
    super(handlerName);
    Preconditions.checkNotNull(network);
    Preconditions.checkNotNull(authentication);
    this.network = network;
    this.authentication = authentication;
  }

  public void contextActive(C ctx) throws Exception {
    checkState(State.NEW);
  }

  @Override
  public void handle(C ctx, LoginHandshakeMessage msg) throws Exception {
    checkAndSetState(State.NEW, State.LOGIN);

    if (msg.getProtocolVersion() > Protocol.VERSION) {
      throw new ProtocolException("Router has outdated protocol version");
    } else if (msg.getProtocolVersion() < Protocol.VERSION) {
      throw new ProtocolException("Client has outdated protocol version");
    }

    this.protocolVersion = msg.getProtocolVersion();
    this.id = ProtocolUtils.convert(msg.getId());
    this.name = msg.getName();

    Preconditions.checkState(this.id != null);
    
    send(ctx, LoginChallengeMessage.newBuilder().setToken(this.authentication.getChallenge()).build());
  }

  protected abstract void send(C ctx, LoginChallengeMessage msg) throws Exception;

  @Override
  public void handle(C ctx, LoginResponseMessage msg) throws Exception {
    checkAndSetState(State.LOGIN, State.AUTH);

    if (!this.authentication.checkResponse(msg.getToken())) {
      throw new HandshakeException("invalid login");
    }

    LoginSuccessMessage.Builder b = LoginSuccessMessage.newBuilder();
    b.setRouterId(ProtocolUtils.convert(this.network.getHome().getId()));
    b.setTopology(this.network.getTopologyMessage());
    send(ctx, b.build());

  }

  protected abstract void send(C ctx, LoginSuccessMessage msg) throws Exception;

  @Override
  public void handle(C ctx, LoginUpgradeMessage msg) throws Exception {
    checkState(State.AUTH);
    upgrade(ctx, msg);
  }

  protected abstract void upgrade(C ctx, LoginUpgradeMessage msg) throws Exception;

  @Override
  public void handle(C ctx, LoginChallengeMessage msg) throws Exception {
    throw new ProtocolException("invalid packet");
  }

  @Override
  public void handle(C ctx, LoginSuccessMessage msg) throws Exception {
    throw new ProtocolException("invalid packet");
  }

}
