package de.rennschnitzel.backbone.netty;

import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.AuthChallenge;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.AuthResponse;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.AuthSuccess;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.Login;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.Connected;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.Disconnected;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ErrorMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.Message;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.Packet;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

public final class PacketUtil {

  private PacketUtil() {}


  public static final ChannelFuture writeAndFlush(final Channel ch,//
 final Packet packet) {
    return ch.writeAndFlush(packet);
  }


  public static final ChannelFuture writeAndFlush(final Channel ch,//
 final Packet.Builder builder) {
    return writeAndFlush(ch, builder.build());
  }

  /*
   * public static final ChannelFuture write(final Channel ch,//
 final TransportProto.Packet packet) {
   * return ch.write(packet); }
   * 
   * 
   * public static final ChannelFuture write(final Channel ch,//
 final TransportProto.Packet.Builder
   * builder) { return write(ch, builder.build()); }
   */

  // ******************************************************************************
  // Util
  public static final ChannelFuture writeAndFlush(final Channel ch,//

      final ErrorMessage.Builder value) {
    return writeAndFlush(ch, Packet.newBuilder().setError(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch,//
 final ErrorMessage value) {
    return writeAndFlush(ch, Packet.newBuilder().setError(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch,//

      final CloseMessage.Builder value) {
    return writeAndFlush(ch, Packet.newBuilder().setClose(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch,//
 final CloseMessage value) {
    return writeAndFlush(ch, Packet.newBuilder().setClose(value));
  }


  // ******************************************************************************
  // Handshake
  public static final ChannelFuture writeAndFlush(final Channel ch,//
 final Login.Builder value) {
    return writeAndFlush(ch, Packet.newBuilder().setLogin(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch,//
 final Login value) {
    return writeAndFlush(ch, Packet.newBuilder().setLogin(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch,//

      final AuthChallenge.Builder value) {
    return writeAndFlush(ch, Packet.newBuilder().setAuthChallenge(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch,//
 final AuthChallenge value) {
    return writeAndFlush(ch, Packet.newBuilder().setAuthChallenge(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch,//

      final AuthResponse.Builder value) {
    return writeAndFlush(ch, Packet.newBuilder().setAuthResponse(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch,//
 final AuthResponse value) {
    return writeAndFlush(ch, Packet.newBuilder().setAuthResponse(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch,//

      final AuthSuccess.Builder value) {
    return writeAndFlush(ch, Packet.newBuilder().setAuthSuccess(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch,//
 final AuthSuccess value) {
    return writeAndFlush(ch, Packet.newBuilder().setAuthSuccess(value));
  }

  // ******************************************************************************
  // Transport
  public static final ChannelFuture writeAndFlush(final Channel ch,//
 final Message.Builder value) {
    return writeAndFlush(ch, Packet.newBuilder().setMessage(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch,//
 final Message value) {
    return writeAndFlush(ch, Packet.newBuilder().setMessage(value));
  }


  // ******************************************************************************
  // Network

  public static final ChannelFuture writeAndFlush(final Channel ch,//
 final Connected.Builder value) {
    return writeAndFlush(ch, Packet.newBuilder().setConnected(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch,//
 final Connected value) {
    return writeAndFlush(ch, Packet.newBuilder().setConnected(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch,//

      final Disconnected.Builder value) {
    return writeAndFlush(ch, Packet.newBuilder().setDisconnected(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch,//
 final Disconnected value) {
    return writeAndFlush(ch, Packet.newBuilder().setDisconnected(value));
  }

}
