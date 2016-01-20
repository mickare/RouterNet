package de.rennschnitzel.net.netty.handshake;

import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.handshake.AbstractHandshakeHandler;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NettyHandshakeAdapter<C extends Connection>
    extends SimpleChannelInboundHandler<Packet> {

  @NonNull
  private final AbstractHandshakeHandler<C, ChannelHandlerContext> delegate;

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    delegate.fail(cause);
    super.exceptionCaught(ctx, cause);
  }

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx, final Packet packet)
      throws Exception {
    delegate.handle(ctx, packet);
  }

  public String getName() {
    return delegate.getName();
  }


}
