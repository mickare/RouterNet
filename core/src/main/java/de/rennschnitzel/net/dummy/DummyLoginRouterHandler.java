package de.rennschnitzel.net.dummy;

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
import io.netty.util.concurrent.Future;

public class DummyLoginRouterHandler extends LoginRouterHandler<DummyConnection> {

  private final PacketHandler<DummyConnection> upgradeHandler;

  public DummyLoginRouterHandler(AbstractNetwork network, AuthenticationRouter authentication,
      PacketHandler<DummyConnection> upgradeHandler) {
    this(DummyLoginRouterHandler.class.getName(), network, authentication, upgradeHandler);
  }

  public DummyLoginRouterHandler(String handlerName, AbstractNetwork network, AuthenticationRouter authentication,
      PacketHandler<DummyConnection> upgradeHandler) {
    super(handlerName, network, authentication);
    Preconditions.checkNotNull(upgradeHandler);
    this.upgradeHandler = upgradeHandler;
  }

  @Override
  protected Future<?> send(DummyConnection ctx, LoginChallengeMessage msg) throws Exception {
    return ctx.send(Packet.newBuilder().setLoginChallenge(msg));
  }

  @Override
  protected Future<?> send(DummyConnection ctx, LoginSuccessMessage msg) throws Exception {
    return ctx.send(Packet.newBuilder().setLoginSuccess(msg));
  }

  @Override
  protected void upgrade(DummyConnection ctx, LoginUpgradeMessage msg) throws HandshakeException {
    ctx.setHandler(upgradeHandler);
    this.setSuccess();
    this.getConnectionPromise().setSuccess(ctx);
  }

  @Override
  protected Future<?> send(DummyConnection ctx, CloseMessage msg) {
    return ctx.send(Packet.newBuilder().setClose(msg));
  }

}
