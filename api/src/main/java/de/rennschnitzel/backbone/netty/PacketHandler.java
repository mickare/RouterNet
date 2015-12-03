package de.rennschnitzel.backbone.netty;

import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.AuthChallenge;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.AuthResponse;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.AuthSuccess;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.Login;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.Connected;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.Disconnected;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.ServerUpdate;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ErrorMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.Message;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.Packet;
import de.rennschnitzel.backbone.netty.exception.ProtocolException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public abstract class PacketHandler extends SimpleChannelInboundHandler<Packet> {

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx, final Packet packet)
      throws Exception {
    switch (packet.getValueCase()) {
      case ERROR:
        handle(ctx, packet.getError());
        break;
      case LOGIN:
        handle(ctx, packet.getLogin());
        break;
      case AUTHCHALLENGE:
        handle(ctx, packet.getAuthChallenge());
        break;
      case AUTHRESPONSE:
        handle(ctx, packet.getAuthResponse());
        break;
      case AUTHSUCCESS:
        handle(ctx, packet.getAuthSuccess());
        break;
      case CONNECTED:
        handle(ctx, packet.getConnected());
        break;
      case DISCONNECTED:
        handle(ctx, packet.getDisconnected());
        break;
      case MESSAGE:
        handle(ctx, packet.getMessage());
        break;
      case UPDATE:
        handle(ctx, packet.getUpdate());
      default:
        handleUndefined(ctx, packet);
    }
  }


  protected abstract void handle(ChannelHandlerContext ctx, ErrorMessage error) throws Exception;

  protected abstract void handle(ChannelHandlerContext ctx, Login login) throws Exception;

  protected abstract void handle(ChannelHandlerContext ctx, AuthChallenge authChallenge)
      throws Exception;

  protected abstract void handle(ChannelHandlerContext ctx, AuthResponse authResponse)
      throws Exception;

  protected abstract void handle(ChannelHandlerContext ctx, AuthSuccess authSuccess)
      throws Exception;

  protected abstract void handle(ChannelHandlerContext ctx, Connected connected) throws Exception;

  protected abstract void handle(ChannelHandlerContext ctx, Disconnected disconnected)
      throws Exception;

  protected abstract void handle(ChannelHandlerContext ctx, Message message) throws Exception;

  protected abstract void handle(ChannelHandlerContext ctx, ServerUpdate update)throws Exception;
  
  protected void handleUndefined(ChannelHandlerContext ctx, Packet undefined)
      throws ProtocolException {
    throw new ProtocolException("Invalid or unknown packet!");
  }

}
