package de.rennschnitzel.net.netty;

import de.rennschnitzel.net.core.packet.PacketHandler;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public abstract class NettyPacketHandler extends SimpleChannelInboundHandler<Packet>
    implements PacketHandler<ChannelHandlerContext> {

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx, final Packet packet)
      throws Exception {
    this.handle(ctx, packet);
  }

}
