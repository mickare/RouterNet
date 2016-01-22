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
import io.netty.util.concurrent.Future;
import lombok.Getter;

public abstract class LoginClientHandler<C> extends LoginHandler<C> {

  private final AuthenticationClient authentication;

  @Getter
  private UUID id = null;
  @Getter
  private String name = null;

  @Getter
  private LoginSuccessMessage finalMessage;

  public LoginClientHandler(String handlerName, AbstractNetwork network, AuthenticationClient authentication) {
    super(handlerName, network);
    Preconditions.checkNotNull(authentication);
    this.authentication = authentication;
  }

  @Override
  public void channelActive(C ctx) throws Exception {
    checkAndSetState(State.NEW, State.LOGIN);
    LoginHandshakeMessage.Builder msg = LoginHandshakeMessage.newBuilder();
    msg.setProtocolVersion(Protocol.VERSION);
    msg.setId(ProtocolUtils.convert(getNetwork().getHome().getId()));
    Optional<String> name = getNetwork().getHome().getName();
    if (name.isPresent()) {
      msg.setName(name.get());
    }
    addFailListener(ctx, send(ctx, msg.build()));
  }

  protected abstract Future<?> send(C ctx, LoginHandshakeMessage handshake) throws Exception;

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
    addFailListener(ctx, send(ctx, LoginResponseMessage.newBuilder().setToken(response).build()));
  }

  protected abstract Future<?> send(C ctx, LoginResponseMessage response) throws Exception;

  @Override
  public void handle(C ctx, LoginSuccessMessage msg) throws Exception {
    checkState(State.AUTH);
    this.finalMessage = msg;
    this.id = ProtocolUtils.convert(msg.getRouterId());
    this.name = msg.getRouterName();
    Preconditions.checkState(this.id != null);
    upgrade(ctx, msg);
  }

  protected abstract void upgrade(C ctx, LoginSuccessMessage msg) throws Exception;

  protected abstract Future<?> send(C ctx, LoginUpgradeMessage upgrade) throws IOException;

  @Override
  public void handle(C ctx, LoginUpgradeMessage msg) throws Exception {
    throw new ProtocolException("invalid packet");
  }

  @Override
  public void registerNodes() {
    this.getNetwork().updateNodes(this.getFinalMessage().getTopology());
  }

}
