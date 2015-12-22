package de.rennschnitzel.backbone.net;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.net.node.HomeNode;
import de.rennschnitzel.backbone.net.packet.PacketHandler;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.Packet;
import lombok.Getter;

public class ConnectionForTesting extends Connection {

  @Getter
  private final HomeNode home;
  @Getter
  private boolean closed = false;

  private final PacketHandler<Connection> handler;

  public ConnectionForTesting(HomeNode home, PacketHandler<Connection> handler) {
    Preconditions.checkNotNull(home);
    Preconditions.checkNotNull(handler);
    this.home = home;
    this.handler = handler;
  }

  @Override
  public void send(Packet packet) {
    try {
      handler.handle(this, packet);
    } catch (Exception pe) {
      throw new RuntimeException(pe);
    }
  }


}
