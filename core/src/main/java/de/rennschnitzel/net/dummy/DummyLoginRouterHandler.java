package de.rennschnitzel.net.dummy;

import java.io.IOException;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.login.AuthenticationRouter;
import de.rennschnitzel.net.core.login.LoginRouterHandler;
import de.rennschnitzel.net.core.packet.PacketHandler;
import de.rennschnitzel.net.exception.HandshakeException;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginChallengeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginSuccessMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginUpgradeMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;

public class DummyLoginRouterHandler extends LoginRouterHandler<DummyConnection> {

  private final PacketHandler<DummyConnection> upgradeHandler;

  public DummyLoginRouterHandler(String handlerName, AbstractNetwork network, AuthenticationRouter authentication,
      PacketHandler<DummyConnection> upgradeHandler) {
    super(handlerName, network, authentication);
    Preconditions.checkNotNull(upgradeHandler);
    this.upgradeHandler = upgradeHandler;
  }

  @Override
  protected void send(DummyConnection ctx, LoginChallengeMessage msg) throws Exception {
    ctx.send(Packet.newBuilder().setLoginChallenge(msg));
  }

  @Override
  protected void send(DummyConnection ctx, LoginSuccessMessage msg) throws Exception {
    ctx.send(Packet.newBuilder().setLoginSuccess(msg));
  }

  @Override
  protected void upgrade(DummyConnection ctx, LoginUpgradeMessage msg) throws HandshakeException {
    this.setSuccess();
    ctx.setHandler(upgradeHandler);
  }

  @Override
  protected void send(DummyConnection ctx, CloseMessage msg) {
    if (ctx.isActive()) {
      try {
        ctx.send(Packet.newBuilder().setClose(msg));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

}
