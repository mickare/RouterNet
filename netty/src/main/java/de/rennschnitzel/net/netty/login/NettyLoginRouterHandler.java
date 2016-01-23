package de.rennschnitzel.net.netty.login;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.login.AuthenticationRouter;
import de.rennschnitzel.net.core.login.LoginRouterHandler;
import de.rennschnitzel.net.exception.ConnectionException;
import de.rennschnitzel.net.netty.PacketUtil;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginChallengeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginSuccessMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginUpgradeMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ErrorMessage;
import de.rennschnitzel.net.util.FutureUtils;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;

public class NettyLoginRouterHandler extends LoginRouterHandler<ChannelHandlerContext> {


  public NettyLoginRouterHandler(AbstractNetwork network, AuthenticationRouter authentication) {
    super("NettyLoginRouterHandler", network, authentication);
  }

  @Override
  protected ChannelFuture send(ChannelHandlerContext ctx, LoginChallengeMessage msg)
      throws Exception {
    return PacketUtil.writeAndFlush(ctx.channel(), msg);
  }

  @Override
  protected ChannelFuture send(ChannelHandlerContext ctx, LoginSuccessMessage msg)
      throws Exception {
    return PacketUtil.writeAndFlush(ctx.channel(), msg);
  }

  @Override
  protected void upgrade(ChannelHandlerContext ctx, LoginUpgradeMessage msg) throws Exception {
    Preconditions.checkNotNull(msg.getNode());
    this.setSuccess();
  }

  @Override
  protected ChannelFuture send(ChannelHandlerContext ctx, CloseMessage msg) {
    return PacketUtil.writeAndFlush(ctx.channel(), msg).addListener(ChannelFutureListener.CLOSE);
  }

  @Override
  public boolean isContextActive() {
    if (this.getContext() != null) {
      return this.getContext().channel().isActive();
    }
    return false;
  }

  @Override
  public Future<?> tryDisconnect(String reason) {
    if (!this.isDone()) {
      this.fail(new ConnectionException(ErrorMessage.Type.UNAVAILABLE, "shutdown"));
    }
    if (this.getContext() != null) {
      return this.getContext().close();
    }
    return FutureUtils.SUCCESS;
  }
}
