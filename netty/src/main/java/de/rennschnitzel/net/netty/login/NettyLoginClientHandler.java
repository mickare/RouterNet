package de.rennschnitzel.net.netty.login;

import java.io.IOException;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.login.AuthenticationClient;
import de.rennschnitzel.net.core.login.LoginClientHandler;
import de.rennschnitzel.net.netty.PacketUtil;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginHandshakeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginResponseMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginSuccessMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginUpgradeMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import io.netty.channel.ChannelHandlerContext;

public class NettyLoginClientHandler extends LoginClientHandler<ChannelHandlerContext> {

  public NettyLoginClientHandler(AbstractNetwork network, AuthenticationClient authentication) {
    super("NettyLoginClientHandler", network, authentication);
  }

  @Override
  protected void send(ChannelHandlerContext ctx, LoginHandshakeMessage handshake) throws Exception {
    PacketUtil.writeAndFlush(ctx.channel(), handshake);
  }

  @Override
  protected void send(ChannelHandlerContext ctx, LoginResponseMessage response) throws Exception {
    PacketUtil.writeAndFlush(ctx.channel(), response);
  }

  @Override
  protected void upgrade(ChannelHandlerContext ctx, LoginSuccessMessage msg) throws Exception {
    this.send(ctx,
        LoginUpgradeMessage.newBuilder().setNode(this.getNetwork().getHome().toProtocol()).build());
    this.setSuccess();
  }

  @Override
  protected void send(ChannelHandlerContext ctx, LoginUpgradeMessage upgrade) throws IOException {
    PacketUtil.writeAndFlush(ctx.channel(), upgrade);
  }

  @Override
  protected void send(ChannelHandlerContext ctx, CloseMessage msg) {
    if (ctx.channel().isActive()) {
      PacketUtil.writeAndFlush(ctx.channel(), msg);
    }
  }

}
