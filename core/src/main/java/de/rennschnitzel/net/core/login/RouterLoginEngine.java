package de.rennschnitzel.net.core.login;

import java.util.Optional;
import java.util.UUID;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.Protocol;
import de.rennschnitzel.net.ProtocolUtils;
import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.event.LoginSuccessEvent.ClientLoginSuccessEvent;
import de.rennschnitzel.net.exception.HandshakeException;
import de.rennschnitzel.net.exception.ProtocolException;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginChallengeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginHandshakeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginResponseMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginSuccessMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginUpgradeMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeMessage;
import lombok.Getter;

public class RouterLoginEngine extends LoginEngine {

  private final RouterAuthentication authentication;

  @Getter
  private int protocolVersion = -1;
  @Getter
  private UUID loginId = null;
  @Getter
  private String loginName = null;

  @Getter
  private LoginUpgradeMessage finalMessage = null;

  public RouterLoginEngine(AbstractNetwork network, RouterAuthentication authentication) {
    super(network);
    Preconditions.checkNotNull(authentication);
    this.authentication = authentication;
  }

  @Override
  public void start() throws Exception {
    Preconditions.checkState(this.getChannel() != null);
    checkState(State.NEW);
  }

  @Override
  public void handle(LoginEngine ctx, LoginHandshakeMessage msg) throws Exception {
    checkAndSetState(State.NEW, State.LOGIN);

    if (msg.getProtocolVersion() > Protocol.VERSION) {
      throw new ProtocolException("Router has outdated protocol version");
    } else if (msg.getProtocolVersion() < Protocol.VERSION) {
      throw new ProtocolException("Client has outdated protocol version");
    }

    this.protocolVersion = msg.getProtocolVersion();
    this.loginId = ProtocolUtils.convert(msg.getId());
    this.loginName = msg.getName();

    Preconditions.checkState(this.loginId != null);

    this.getChannel().writeAndFlush(LoginChallengeMessage.newBuilder().setToken(this.authentication.getChallenge()))
        .addListener(FAIL_LISTENER);
  }

  @Override
  public void handle(LoginEngine ctx, LoginResponseMessage msg) throws Exception {
    checkAndSetState(State.LOGIN, State.AUTH);

    if (!this.authentication.checkResponse(msg.getToken())) {
      this.fail(new HandshakeException("invalid login"));
      return;
    }

    LoginSuccessMessage.Builder b = LoginSuccessMessage.newBuilder();
    b.setRouterId(ProtocolUtils.convert(this.getNetwork().getHome().getId()));
    Optional<String> name = this.getNetwork().getHome().getName();
    if (name.isPresent()) {
      b.setRouterName(name.get());
    }
    b.setTopology(this.getNetwork().getTopologyMessage());
    this.getChannel().writeAndFlush(b).addListener(FAIL_LISTENER);
  }

  @Override
  public void handle(LoginEngine ctx, LoginUpgradeMessage msg) throws Exception {
    checkState(State.AUTH);
    this.finalMessage = msg;
    NodeMessage node = msg.getNode();
    Preconditions.checkState(node != null);
    setSuccess();
  }

  @Override
  public void handle(LoginEngine ctx, LoginChallengeMessage msg) throws Exception {
    throw new ProtocolException("invalid packet");
  }

  @Override
  public void handle(LoginEngine ctx, LoginSuccessMessage msg) throws Exception {
    throw new ProtocolException("invalid packet");
  }

  @Override
  public ClientLoginSuccessEvent newLoginSuccessEvent(Connection connection) {
    return new ClientLoginSuccessEvent(this.getNetwork(), this.getLoginId(), this.getLoginName(), connection, this.finalMessage.getNode());
  }

}
