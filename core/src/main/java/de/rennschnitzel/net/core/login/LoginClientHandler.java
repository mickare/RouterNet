package de.rennschnitzel.net.core.login;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;

import de.rennschnitzel.net.Protocol;
import de.rennschnitzel.net.ProtocolUtils;
import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.exception.ProtocolException;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginChallengeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginHandshakeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginResponseMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginSuccessMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginUpgradeMessage;
import lombok.Getter;

public abstract class LoginClientHandler<C> extends LoginHandler<C> {

  @Getter
  private final AbstractNetwork network;
  private final AuthenticationClient authentication;

  @Getter
  private UUID id = null;

  public LoginClientHandler(String handlerName, AbstractNetwork network, AuthenticationClient authentication) {
    super(handlerName);
    Preconditions.checkNotNull(network);
    Preconditions.checkNotNull(authentication);
    this.network = network;
    this.authentication = authentication;
  }

  public void contextActive(C ctx) throws Exception {
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

  protected abstract void send(C ctx, LoginHandshakeMessage handshake) throws Exception;

  @Override
  public void handle(C ctx, LoginHandshakeMessage msg) throws Exception {
    throw new ProtocolException("invalid packet");
  }

  @Override
  public void handle(C ctx, LoginResponseMessage msg) throws Exception {
    throw new ProtocolException("invalid packet");
  }

  @Override
  public void handle(C ctx, LoginChallengeMessage msg) throws Exception {
    checkAndSetState(State.LOGIN, State.AUTH);
    ByteString response = this.authentication.calculateResponse(msg.getToken());
    send(ctx, LoginResponseMessage.newBuilder().setToken(response).build());
  }

  protected abstract void send(C ctx, LoginResponseMessage response) throws Exception;

  @Override
  public void handle(C ctx, LoginSuccessMessage msg) throws Exception {
    checkState(State.AUTH);
    this.id = ProtocolUtils.convert(msg.getRouterId());
    Preconditions.checkState(this.id != null);
    upgrade(ctx, msg);
  }

  protected abstract void upgrade(C ctx, LoginSuccessMessage msg) throws Exception;

  protected abstract void send(C ctx, LoginUpgradeMessage upgrade) throws IOException;

  @Override
  public void handle(C ctx, LoginUpgradeMessage msg) throws Exception {
    throw new ProtocolException("invalid packet");
  }

}
