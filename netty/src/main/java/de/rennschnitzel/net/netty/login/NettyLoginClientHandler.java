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

public abstract class NettyLoginClientHandler extends LoginClientHandler<ChannelHandlerContext> {

  public NettyLoginClientHandler(String handlerName, AbstractNetwork network,
      AuthenticationClient authentication) {
    super(handlerName, network, authentication);
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
    upgradeConnection(ctx);
    this.setSuccess();
  }

  protected abstract void upgradeConnection(ChannelHandlerContext ctx) throws Exception;

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
