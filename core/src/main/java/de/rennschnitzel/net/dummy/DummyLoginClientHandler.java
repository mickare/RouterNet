package de.rennschnitzel.net.dummy;

import java.io.IOException;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.login.AuthenticationClient;
import de.rennschnitzel.net.core.login.LoginClientHandler;
import de.rennschnitzel.net.core.packet.PacketHandler;
import de.rennschnitzel.net.exception.ConnectionException;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginHandshakeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginResponseMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginSuccessMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginUpgradeMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ErrorMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import de.rennschnitzel.net.util.FutureUtils;
import io.netty.util.concurrent.Future;

public class DummyLoginClientHandler extends LoginClientHandler<DummyConnection> {

  private final PacketHandler<DummyConnection> upgradeHandler;

  public DummyLoginClientHandler(AbstractNetwork network, AuthenticationClient authentication,
      PacketHandler<DummyConnection> upgradeHandler) {
    this(DummyLoginClientHandler.class.getName(), network, authentication, upgradeHandler);
  }


  public DummyLoginClientHandler(String handlerName, AbstractNetwork network, AuthenticationClient authentication,
      PacketHandler<DummyConnection> upgradeHandler) {
    super(handlerName, network, authentication);
    Preconditions.checkNotNull(upgradeHandler);
    this.upgradeHandler = upgradeHandler;
  }

  @Override
  protected Future<?> send(DummyConnection ctx, LoginHandshakeMessage handshake) throws Exception {
    return ctx.send(Packet.newBuilder().setLoginHandshake(handshake));
  }

  @Override
  protected Future<?> send(DummyConnection ctx, LoginResponseMessage response) throws Exception {
    return ctx.send(Packet.newBuilder().setLoginResponse(response));
  }

  @Override
  protected void upgrade(DummyConnection ctx, LoginSuccessMessage msg) throws Exception {
    this.send(ctx, LoginUpgradeMessage.newBuilder().setNode(this.getNetwork().getHome().toProtocol()).build()).addListener(f -> {
      if (f.isSuccess()) {
        ctx.setHandler(upgradeHandler);
        this.setSuccess();
        this.getConnectionPromise().setSuccess(ctx);
      } else {
        this.fail(ctx, f.cause());
      }
    });
  }

  @Override
  protected Future<?> send(DummyConnection ctx, LoginUpgradeMessage upgrade) throws IOException {
    return ctx.send(Packet.newBuilder().setLoginUpgrade(upgrade));
  }

  @Override
  protected Future<?> send(DummyConnection ctx, CloseMessage msg) {
    return ctx.send(Packet.newBuilder().setClose(msg));
  }


  @Override
  public boolean isChannelActive() {
    if (this.getContext() != null) {
      return this.getContext().isActive();
    }
    return false;
  }

  @Override
  public Future<?> tryDisconnect(String reason) {
    if (!this.isDone()) {
      this.fail(new ConnectionException(ErrorMessage.Type.UNAVAILABLE, "shutdown"));
    }
    if (this.getContext() != null) {
      return this.getContext().disconnect(reason);
    }
    return FutureUtils.SUCCESS;
  }

}
