package de.rennschnitzel.net.core;

import java.util.UUID;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.packet.PacketWriter;
import de.rennschnitzel.net.exception.ConnectionException;
import de.rennschnitzel.net.netty.ChannelWrapper;
import de.rennschnitzel.net.protocol.TransportProtocol;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class Connection implements PacketWriter<ChannelFuture> {

  @Getter
  private final AbstractNetwork network;
  @Getter
  private final UUID peerId;
  @Getter
  private final ChannelWrapper channel;

  @Getter
  @Setter
  @NonNull
  private CloseMessage closeMessage = null;

  public Connection(AbstractNetwork network, UUID peerId, ChannelWrapper channel) {
    Preconditions.checkNotNull(network);
    Preconditions.checkNotNull(peerId);
    Preconditions.checkNotNull(channel);
    this.network = network;
    this.peerId = peerId;
    this.channel = channel;
  }

  public boolean isOpen() {
    return channel.isOpen();
  }

  public boolean isActive() {
    return channel.isActive();
  }

  public Future<?> disconnect(CloseMessage msg) {
    return channel.writeAndFlush(msg);
  }


  public Future<?> disconnect(CloseMessage.Builder builder) {
    return this.disconnect(builder.build());
  }

  public Future<?> disconnect(String reason) {
    return this.disconnect(CloseMessage.newBuilder().setNormal(reason));
  }


  @Override
  public void flush() {
    channel.flush();
  }


  @Override
  public void writeFast(Packet packet) {
    channel.writeFast(packet);
  }

  @Override
  public ChannelFuture write(Packet packet) {
    return channel.write(packet);
  }


  @Override
  public void writeAndFlushFast(Packet packet) {
    channel.writeAndFlushFast(packet);
  }

  @Override
  public ChannelFuture writeAndFlush(Packet packet) {
    return channel.writeAndFlush(packet);
  }

  @Override
  public String toString() {
    return "Connection[" + this.peerId + "]";
  }

  public void receive(Connection con, TransportProtocol.TunnelRegister msg) throws ConnectionException {
    this.network.receiveTunnelRegister(msg);
  }

}
