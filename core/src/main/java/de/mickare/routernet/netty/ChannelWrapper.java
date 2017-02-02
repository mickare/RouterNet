package de.mickare.routernet.netty;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Optional;

import de.mickare.routernet.core.packet.PacketWriter;
import de.mickare.routernet.protocol.TransportProtocol.Packet;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public @RequiredArgsConstructor class ChannelWrapper implements PacketWriter<ChannelFuture> {

  private @Getter final Channel channel;

  public SocketAddress getRemoteAddress() {
    return channel.remoteAddress();
  }

  public SocketAddress getLocalAddress() {
    return channel.remoteAddress();
  }

  public Optional<InetAddress> getRemoteIp() {
    SocketAddress addr = this.getRemoteAddress();
    if (addr instanceof InetSocketAddress) {
      return Optional.ofNullable(((InetSocketAddress) addr).getAddress());
    }
    return Optional.empty();
  }

  public Optional<Integer> getRemotePort() {
    SocketAddress addr = this.getRemoteAddress();
    if (addr instanceof InetSocketAddress) {
      return Optional.ofNullable(((InetSocketAddress) addr).getPort());
    }
    return Optional.empty();
  }

  public boolean isActive() {
    return channel.isActive();
  }

  public boolean isOpen() {
    return channel.isOpen();
  }

  public ChannelFuture close() {
    return channel.close();
  }

  @Override
  public void flush() {
    channel.flush();
  }

  @Override
  public ChannelFuture write(Packet packet) {
    return channel.write(packet);
  }

  @Override
  public ChannelFuture writeAndFlush(Packet packet) {
    return channel.writeAndFlush(packet);
  }

  @Override
  public void writeFast(Packet packet) {
    channel.write(packet, channel.voidPromise());
  }

  @Override
  public void writeAndFlushFast(Packet packet) {
    channel.writeAndFlush(packet, channel.voidPromise());
  }

}
