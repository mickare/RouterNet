package de.rennschnitzel.net.dummy;

import java.io.IOException;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.login.AuthenticationClient;
import de.rennschnitzel.net.core.login.LoginClientHandler;
import de.rennschnitzel.net.core.packet.PacketHandler;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginHandshakeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginResponseMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginSuccessMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginUpgradeMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import lombok.Getter;

public class DummyLoginClientHandler extends LoginClientHandler<DummyConnection> {

  private final PacketHandler<DummyConnection> upgradeHandler;


  public DummyLoginClientHandler(String handlerName, AbstractNetwork network, AuthenticationClient authentication,
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
  protected void upgrade(DummyConnection ctx, LoginSuccessMessage msg) throws Exception {
    this.send(ctx, LoginUpgradeMessage.newBuilder().setNode(this.getNetwork().getHome().toProtocol()).build());
    ctx.setHandler(upgradeHandler);
    this.setSuccess();
  }

  @Override
  protected void send(DummyConnection ctx, LoginUpgradeMessage upgrade) throws IOException {
    ctx.send(Packet.newBuilder().setLoginUpgrade(upgrade));
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
