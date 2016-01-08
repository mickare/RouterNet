package de.rennschnitzel.backbone.client;

import java.util.UUID;
import java.util.logging.Level;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.api.network.Connection;
import de.rennschnitzel.backbone.api.network.RouterInfo;
import de.rennschnitzel.backbone.client.util.UUIDContainer;
import de.rennschnitzel.backbone.exception.ConnectionException;
import de.rennschnitzel.backbone.exception.HandshakeException;
import de.rennschnitzel.backbone.exception.NotConnectedException;
import de.rennschnitzel.backbone.exception.ProtocolException;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.AuthChallenge;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.AuthResponse;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.AuthSuccess;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.Login;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.Connected;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.Disconnected;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.ServerUpdate;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ErrorMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.Message;
import de.rennschnitzel.backbone.netty.NettyPacketHandler;
import de.rennschnitzel.backbone.netty.PacketUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RouterConnection extends NettyPacketHandler implements Connection {

  @NonNull
  private final AuthSuccess auth;

  @Getter
  @NonNull
  private final Client client;
  @Getter
  @NonNull
  private final RouterInfo router;
  private ChannelHandlerContext ctx = null;


  @Override
  public UUID getRouterUUID() {
    return UUIDContainer.of(auth.getRouter().getId());
  }



  @Override
  public UUID getClientUUID() {
    return UUIDContainer.of(auth.getClient().getId());
  }


  @Override
  public synchronized void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    Preconditions.checkState(this.ctx == null, "Already used handler!");
    this.ctx = ctx;
  }

  @Override
  protected void handle(ChannelHandlerContext ctx, ErrorMessage error) throws Exception {
    boolean fatal = true;
    ConnectionException e = null;
    switch (error.getType()) {
      case PROTOCOL_ERROR:
        e = new ProtocolException(error.getMessage());
        fatal = e.isFatal();
        break;
      default:
        e = new ConnectionException(error);
        fatal = e.isFatal();
    }
    client.getLogger().log(Level.WARNING, "Remote Error: " + router.getName(), e);
    if (fatal)
      ctx.close();
  }

  @Override
  protected void handle(ChannelHandlerContext ctx, Login login) throws Exception {
    throw new HandshakeException("Invalid or unknown packet!");
  }

  @Override
  protected void handle(ChannelHandlerContext ctx, AuthChallenge authChallenge) throws Exception {
    throw new HandshakeException("Invalid or unknown packet!");
  }

  @Override
  protected void handle(ChannelHandlerContext ctx, AuthResponse authResponse) throws Exception {
    throw new HandshakeException("Invalid or unknown packet!");
  }

  @Override
  protected void handle(ChannelHandlerContext ctx, AuthSuccess authSuccess) throws Exception {
    throw new HandshakeException("Invalid or unknown packet!");
  }

  @Override
  protected void handle(ChannelHandlerContext ctx, Connected connected) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  protected void handle(ChannelHandlerContext ctx, Disconnected disconnected) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  protected void handle(ChannelHandlerContext ctx, Message message) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  protected void handle(ChannelHandlerContext ctx, ServerUpdate update) throws Exception {
    // TODO Auto-generated method stub

  }



  @Override
  public boolean isOpen() {
    return ctx.channel().isOpen();
  }

  @Override
  public boolean isActive() {
    return ctx.channel().isActive();
  }

  @Override
  public ChannelFuture close() {
    return this.close(CloseMessage.newBuilder().setNormal("no reason").build());
  }

  @Override
  public ChannelFuture close(CloseMessage packet) {
    if (isActive()) {
      PacketUtil.writeAndFlush(ctx.channel(), packet);
    }
    return ctx.close();
  }

  @Override
  public void send(ErrorMessage packet) throws NotConnectedException {
    if (packet.getFatal()) {
      close(CloseMessage.newBuilder().setError(packet).build());
    } else {
      PacketUtil.writeAndFlush(ctx.channel(), packet);
    }
  }

  @Override
  public void send(Message packet) throws NotConnectedException {
    PacketUtil.writeAndFlush(ctx.channel(), packet);
  }



}
