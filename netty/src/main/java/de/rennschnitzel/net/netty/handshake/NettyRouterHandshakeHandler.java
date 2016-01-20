package de.rennschnitzel.net.netty.handshake;

import java.io.IOException;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.handshake.RouterAuthentication;
import de.rennschnitzel.net.core.handshake.RouterHandshakeHandler;
import de.rennschnitzel.net.netty.PacketUtil;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginChallengeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginSuccessMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginUpgradeMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import io.netty.channel.ChannelHandlerContext;

public abstract class NettyRouterHandshakeHandler<C extends Connection>
    extends RouterHandshakeHandler<C, ChannelHandlerContext> {

  public NettyRouterHandshakeHandler(String handlerName, AbstractNetwork network,
      RouterAuthentication authentication) {
    super(handlerName, network, authentication);
  }

  @Override
  protected void send(ChannelHandlerContext ctx, LoginChallengeMessage msg) throws Exception {
    PacketUtil.writeAndFlush(ctx.channel(), msg);
  }

  @Override
  protected void send(ChannelHandlerContext ctx, LoginSuccessMessage msg) throws Exception {
    PacketUtil.writeAndFlush(ctx.channel(), msg);
  }

  @Override
  protected void upgrade(ChannelHandlerContext ctx, LoginUpgradeMessage msg) throws Exception {
    this.setSuccess(upgradeConnection(ctx, msg));
  }

  protected abstract C upgradeConnection(ChannelHandlerContext ctx, LoginUpgradeMessage msg)
      throws Exception;

  @Override
  protected void send(ChannelHandlerContext ctx, CloseMessage msg) throws IOException {
    PacketUtil.writeAndFlush(ctx.channel(), msg);
  }

  @Override
  public boolean isOpen() {
    return this.getContext().channel().isOpen();
  }

  @Override
  public boolean isActive() {
    return this.getContext().channel().isActive();
  }
}
