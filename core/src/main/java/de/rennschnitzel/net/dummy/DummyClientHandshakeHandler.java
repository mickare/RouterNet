package de.rennschnitzel.net.dummy;

import java.io.IOException;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.handshake.ClientAuthentication;
import de.rennschnitzel.net.core.handshake.ClientHandshakeHandler;
import de.rennschnitzel.net.core.packet.PacketHandler;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginHandshakeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginResponseMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginSuccessMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginUpgradeMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;

public class DummyClientHandshakeHandler extends ClientHandshakeHandler<DummyConnection, DummyConnection> {

  private final PacketHandler<DummyConnection> upgradeHandler;

  public DummyClientHandshakeHandler(String handlerName, AbstractNetwork network, ClientAuthentication authentication,
      PacketHandler<DummyConnection> upgradeHandler) {
    super(handlerName, network, authentication);
    Preconditions.checkNotNull(upgradeHandler);
    this.upgradeHandler = upgradeHandler;
  }

  @Override
  protected void send(DummyConnection ctx, LoginHandshakeMessage handshake) throws Exception {
    ctx.send(Packet.newBuilder().setLoginHandshake(handshake));
  }

  @Override
  protected void send(DummyConnection ctx, LoginResponseMessage response) throws Exception {
    ctx.send(Packet.newBuilder().setLoginResponse(response));
  }

  @Override
  protected LoginUpgradeMessage upgrade(DummyConnection ctx, LoginSuccessMessage msg) throws Exception {
    this.setSuccess(ctx);
    ctx.setHandler(upgradeHandler);
    return LoginUpgradeMessage.newBuilder().setNode(this.getNetwork().getHome().toProtocol()).build();
  }

  @Override
  protected void send(DummyConnection ctx, LoginUpgradeMessage upgrade) throws IOException {
    ctx.send(Packet.newBuilder().setLoginUpgrade(upgrade));
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
