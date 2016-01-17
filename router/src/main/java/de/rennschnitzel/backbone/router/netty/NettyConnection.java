package de.rennschnitzel.backbone.router.netty;

import java.util.UUID;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.net.Connection;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.Packet;
import de.rennschnitzel.backbone.netty.ConnectionBasePacketAdapter;
import de.rennschnitzel.backbone.router.Router;
import de.rennschnitzel.backbone.router.RouterNetwork;
import lombok.Getter;

public class NettyConnection extends Connection {

  @Getter
  private final Router router;

  @Getter
  private final UUID id;

  @Getter
  private final RouterPacketHandler packetHandler = new RouterPacketHandler();
  @Getter
  private final ConnectionBasePacketAdapter<NettyConnection> protocolHandler;

  public NettyConnection(Router router, UUID id) {
    super(router.getNetwork());
    this.router = router;
    this.id = id;
    protocolHandler = new ConnectionBasePacketAdapter<>(this, packetHandler);

  }

  @Override
  public RouterNetwork getNetwork() {
    return (RouterNetwork) this.getNetwork();
  }

  @Override
  public void send(Packet packet) {
    Preconditions.checkNotNull(packet);
    this.protocolHandler.getContext().channel().write(packet);
  }

  @Override
  public boolean isClosed() {
    return !this.protocolHandler.getContext().channel().isOpen();
  }

}
