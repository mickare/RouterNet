package de.rennschnitzel.net.netty.login;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.login.AuthenticationRouter;
import de.rennschnitzel.net.core.login.LoginRouterHandler;
import de.rennschnitzel.net.netty.PacketUtil;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginChallengeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginSuccessMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginUpgradeMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import io.netty.channel.ChannelHandlerContext;

public abstract class NettyLoginRouterHandler extends LoginRouterHandler<ChannelHandlerContext> {

  public NettyLoginRouterHandler(String handlerName, AbstractNetwork network,
      AuthenticationRouter authentication) {
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
    upgradeConnection(ctx, msg);
    this.setSuccess();
  }

  protected abstract void upgradeConnection(ChannelHandlerContext ctx, LoginUpgradeMessage msg)
      throws Exception;

  @Override
  protected void send(ChannelHandlerContext ctx, CloseMessage msg) {
    if (ctx.channel().isActive()) {
      PacketUtil.writeAndFlush(ctx.channel(), msg);
      }
  }


}
