package de.rennschnitzel.net.router.netty;

import java.util.UUID;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.netty.ConnectionPacketAdapter;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import de.rennschnitzel.net.router.Router;
import de.rennschnitzel.net.router.RouterNetwork;
import lombok.Getter;

public class NettyConnection extends Connection {

  @Getter
  private final Router router;

  @Getter
  private final UUID id;

  @Getter
  private final RouterPacketHandler packetHandler = new RouterPacketHandler();
  @Getter
  private final ConnectionPacketAdapter<NettyConnection> protocolHandler;

  public NettyConnection(Router router, UUID id) {
    super(router.getNetwork());
    this.router = router;
    this.id = id;
    protocolHandler = new ConnectionPacketAdapter<>(this, packetHandler);

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
  public boolean isValid() {
    return !this.protocolHandler.getContext().channel().isOpen();
  }

}
