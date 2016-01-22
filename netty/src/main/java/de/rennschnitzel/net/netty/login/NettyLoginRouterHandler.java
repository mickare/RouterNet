package de.rennschnitzel.net.netty.login;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.login.AuthenticationRouter;
import de.rennschnitzel.net.core.login.LoginRouterHandler;
import de.rennschnitzel.net.netty.PacketUtil;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginChallengeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginSuccessMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginUpgradeMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import io.netty.channel.ChannelHandlerContext;

public class NettyLoginRouterHandler extends LoginRouterHandler<ChannelHandlerContext> {


  public NettyLoginRouterHandler(AbstractNetwork network, AuthenticationRouter authentication) {
    super("NettyLoginRouterHandler", network, authentication);
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
    Preconditions.checkNotNull(msg.getNode());
    this.setSuccess();
  }

  @Override
  protected void send(ChannelHandlerContext ctx, CloseMessage msg) {
    if (ctx.channel().isActive()) {
      PacketUtil.writeAndFlush(ctx.channel(), msg);
    }
  }

}
