package de.rennschnitzel.net.netty.login;

import java.io.IOException;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.login.AuthenticationClient;
import de.rennschnitzel.net.core.login.LoginClientHandler;
import de.rennschnitzel.net.exception.ConnectionException;
import de.rennschnitzel.net.netty.PacketUtil;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginHandshakeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginResponseMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginSuccessMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginUpgradeMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ErrorMessage;
import de.rennschnitzel.net.util.FutureUtils;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;

public class NettyLoginClientHandler extends LoginClientHandler<ChannelHandlerContext> {

  public NettyLoginClientHandler(AbstractNetwork network, AuthenticationClient authentication) {
    super("NettyLoginClientHandler", network, authentication);
  }

  @Override
  protected ChannelFuture send(ChannelHandlerContext ctx, LoginHandshakeMessage handshake)
      throws Exception {
    return PacketUtil.writeAndFlush(ctx.channel(), handshake);
  }

  @Override
  protected ChannelFuture send(ChannelHandlerContext ctx, LoginResponseMessage response)
      throws Exception {
    return PacketUtil.writeAndFlush(ctx.channel(), response);
  }

  @Override
  protected void upgrade(ChannelHandlerContext ctx, final LoginSuccessMessage msg)
      throws Exception {
    this.send(ctx,
        LoginUpgradeMessage.newBuilder().setNode(this.getNetwork().getHome().toProtocol()).build())//
        .addListener(f -> {
          if (f.isSuccess()) {
            this.getNetwork().updateNodes(msg.getTopology());
            this.setSuccess();
          } else {
            this.fail(ctx, f.cause());
          }
        });
  }

  @Override
  protected ChannelFuture send(ChannelHandlerContext ctx, LoginUpgradeMessage upgrade)
      throws IOException {
    return PacketUtil.writeAndFlush(ctx.channel(), upgrade);
  }

  @Override
  protected ChannelFuture send(ChannelHandlerContext ctx, CloseMessage msg) {
    return PacketUtil.writeAndFlush(ctx.channel(), msg).addListener(ChannelFutureListener.CLOSE);
  }


  @Override
  public boolean isChannelActive() {
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
