package de.rennschnitzel.net.netty.handshake;

import java.io.IOException;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.handshake.ClientAuthentication;
import de.rennschnitzel.net.core.handshake.ClientHandshakeHandler;
import de.rennschnitzel.net.netty.PacketUtil;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginHandshakeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginResponseMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginSuccessMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginUpgradeMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import io.netty.channel.ChannelHandlerContext;

public abstract class NettyClientHandshakeHandler<C extends Connection>
    extends ClientHandshakeHandler<C, ChannelHandlerContext> {

  public NettyClientHandshakeHandler(String handlerName, AbstractNetwork network,
      ClientAuthentication authentication) {
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
    this.setSuccess(upgradeConnection(ctx));
  }

  protected abstract C upgradeConnection(ChannelHandlerContext ctx) throws Exception;

  @Override
  protected void send(ChannelHandlerContext ctx, LoginUpgradeMessage upgrade) throws IOException {
    PacketUtil.writeAndFlush(ctx.channel(), upgrade);
  }

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
