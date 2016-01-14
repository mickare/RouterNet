package de.rennschnitzel.backbone.netty;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.net.Connection;
import de.rennschnitzel.backbone.net.packet.BasePacketHandler;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.AuthChallengeMessage;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.AuthResponseMessage;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.AuthSuccessMessage;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.LoginMessage;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.ConnectedMessage;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.DisconnectedMessage;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.ServerUpdateMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ChannelMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ChannelRegister;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureMessage;
import de.rennschnitzel.backbone.netty.NettyPacketHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConnectionBasePacketAdapter<C extends Connection> extends NettyPacketHandler {

  @Getter
  @NonNull
  private final C con;

  @Getter
  @NonNull
  private final BasePacketHandler<C> handler;

  private ChannelHandlerContext ctx = null;

  @Override
  public synchronized void handlerAdded(ChannelHandlerContext ctx) {
    Preconditions.checkNotNull(ctx);
    Preconditions.checkState(this.ctx == null);
    this.ctx = ctx;
  }

  @Override
  public void handle(ChannelHandlerContext ctx, ProcedureMessage msg) throws Exception {
    handler.handle(con, msg);
  }

  @Override
  public void handle(ChannelHandlerContext ctx, ChannelRegister msg) throws Exception {
    handler.handle(con, msg);
  }

  @Override
  public void handle(ChannelHandlerContext ctx, ChannelMessage msg) throws Exception {
    handler.handle(con, msg);
  }

  @Override
  public void handle(ChannelHandlerContext ctx, ServerUpdateMessage msg) throws Exception {
    handler.handle(con, msg);
  }

  @Override
  public void handle(ChannelHandlerContext ctx, DisconnectedMessage msg) throws Exception {
    handler.handle(con, msg);
  }

  @Override
  public void handle(ChannelHandlerContext ctx, ConnectedMessage msg) throws Exception {
    handler.handle(con, msg);
  }

  @Override
  public void handle(ChannelHandlerContext ctx, AuthResponseMessage msg) throws Exception {
    handler.handle(con, msg);
  }

  @Override
  public void handle(ChannelHandlerContext ctx, AuthChallengeMessage msg) throws Exception {
    handler.handle(con, msg);
  }

  @Override
  public void handle(ChannelHandlerContext ctx, AuthSuccessMessage msg) throws Exception {
    handler.handle(con, msg);
  }

  @Override
  public void handle(ChannelHandlerContext ctx, LoginMessage msg) throws Exception {
    handler.handle(con, msg);
  }

  @Override
  public void handle(ChannelHandlerContext ctx, CloseMessage msg) throws Exception {
    handler.handle(con, msg);
  }

}
