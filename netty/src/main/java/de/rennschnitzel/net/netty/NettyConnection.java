package de.rennschnitzel.net.netty;

import java.util.UUID;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.packet.PacketHandler;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.Getter;

public class NettyConnection<N extends AbstractNetwork> extends Connection {


  private final MainHandler<N> mainHandler;

  @Getter
  private final PacketHandler<NettyConnection<N>> packetHandler;

  public NettyConnection(N network, UUID id, MainHandler<N> mainHandler,
      PacketHandler<NettyConnection<N>> packetHandler) {
    super(network, id);
    Preconditions.checkNotNull(mainHandler);
    Preconditions.checkNotNull(packetHandler);
    this.mainHandler = mainHandler;
    this.packetHandler = packetHandler;

  }

  @SuppressWarnings("unchecked")
  @Override
  public N getNetwork() {
    return (N) super.getNetwork();
  }

  @Override
  public ChannelFuture send(Packet packet) {
    return this.mainHandler.sendAndFlush(packet);
  }

  protected void receive(Packet packet) throws Exception {
    this.packetHandler.handle(this, packet);
  }

  @Override
  public boolean isOpen() {
    return this.mainHandler.getContext().channel().isOpen();
  }

  @Override
  public boolean isActive() {
    return this.mainHandler.getContext().channel().isActive();
  }

  @Override
  public void disconnect(CloseMessage msg) {
    send(Packet.newBuilder().setClose(msg).build()).addListener(ChannelFutureListener.CLOSE);
  }

}
