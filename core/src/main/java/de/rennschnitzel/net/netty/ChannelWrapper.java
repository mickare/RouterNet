package de.rennschnitzel.net.netty;

import de.rennschnitzel.net.core.packet.PacketWriter;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


public @RequiredArgsConstructor class ChannelWrapper implements PacketWriter<ChannelFuture> {
  
  
  private @Getter final Channel channel;

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
    channel.writeAndFlush(packet,  channel.voidPromise());
  }

}
