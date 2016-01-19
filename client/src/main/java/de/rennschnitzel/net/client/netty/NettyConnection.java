package de.rennschnitzel.net.client.netty;

import java.util.UUID;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.NetClient;
import de.rennschnitzel.net.Network;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.packet.BasePacketHandler;
import de.rennschnitzel.net.netty.ConnectionBasePacketAdapter;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import lombok.Getter;

public class NettyConnection extends Connection {

  @Getter
  private final NetClient client;

  @Getter
  private final UUID id;

  @Getter
  private final BasePacketHandler<NettyConnection> packetHandler = new BasePacketHandler<>();
  @Getter
  private final ConnectionBasePacketAdapter<NettyConnection> protocolHandler;

  public NettyConnection(NetClient client, UUID id) {
    super(client.getNetwork());
    this.client = client;
    this.id = id;
    protocolHandler = new ConnectionBasePacketAdapter<>(this, packetHandler);

  }

  @Override
  public Network getNetwork() {
    return (Network) this.getNetwork();
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
