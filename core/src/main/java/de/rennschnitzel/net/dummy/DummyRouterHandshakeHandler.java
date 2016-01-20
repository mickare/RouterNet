package de.rennschnitzel.net.dummy;

import java.io.IOException;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.handshake.RouterAuthentication;
import de.rennschnitzel.net.core.handshake.RouterHandshakeHandler;
import de.rennschnitzel.net.core.packet.PacketHandler;
import de.rennschnitzel.net.exception.HandshakeException;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginChallengeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginSuccessMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginUpgradeMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;

public class DummyRouterHandshakeHandler extends RouterHandshakeHandler<DummyConnection, DummyConnection> {

  private final PacketHandler<DummyConnection> upgradeHandler;

  public DummyRouterHandshakeHandler(String handlerName, AbstractNetwork network, RouterAuthentication authentication,
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
    this.setSuccess(ctx);
    ctx.setHandler(upgradeHandler);
  }

  @Override
  protected void send(DummyConnection ctx, CloseMessage msg) throws IOException {
    ctx.send(Packet.newBuilder().setClose(msg));
  }

  @Override
  public boolean isOpen() {
    return !this.getContext().isClosed() && !this.isDone();
  }

  @Override
  public boolean isActive() {
    return this.isOpen() && this.getContext().isActive();
  }

}
