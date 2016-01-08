package de.rennschnitzel.backbone.netty;

import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.AuthChallengeMessage;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.AuthResponseMessage;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.AuthSuccessMessage;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.LoginMessage;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.ConnectedMessage;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.DisconnectedMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ChannelMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ErrorMessage;
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
 final LoginMessage.Builder value) {
    return writeAndFlush(ch, Packet.newBuilder().setLogin(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch,//
 final LoginMessage value) {
    return writeAndFlush(ch, Packet.newBuilder().setLogin(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch,//

      final AuthChallengeMessage.Builder value) {
    return writeAndFlush(ch, Packet.newBuilder().setAuthChallenge(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch,//
 final AuthChallengeMessage value) {
    return writeAndFlush(ch, Packet.newBuilder().setAuthChallenge(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch,//

      final AuthResponseMessage.Builder value) {
    return writeAndFlush(ch, Packet.newBuilder().setAuthResponse(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch,//
 final AuthResponseMessage value) {
    return writeAndFlush(ch, Packet.newBuilder().setAuthResponse(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch,//

      final AuthSuccessMessage.Builder value) {
    return writeAndFlush(ch, Packet.newBuilder().setAuthSuccess(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch,//
 final AuthSuccessMessage value) {
    return writeAndFlush(ch, Packet.newBuilder().setAuthSuccess(value));
  }

  // ******************************************************************************
  // Transport
  public static final ChannelFuture writeAndFlush(final Channel ch,//
 final ChannelMessage.Builder value) {
    return writeAndFlush(ch, Packet.newBuilder().setChannelMessage(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch,//
 final ChannelMessage value) {
    return writeAndFlush(ch, Packet.newBuilder().setChannelMessage(value));
  }


  // ******************************************************************************
  // Network

  public static final ChannelFuture writeAndFlush(final Channel ch,//
 final ConnectedMessage.Builder value) {
    return writeAndFlush(ch, Packet.newBuilder().setConnected(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch,//
 final ConnectedMessage value) {
    return writeAndFlush(ch, Packet.newBuilder().setConnected(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch,//

      final DisconnectedMessage.Builder value) {
    return writeAndFlush(ch, Packet.newBuilder().setDisconnected(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch,//
 final DisconnectedMessage value) {
    return writeAndFlush(ch, Packet.newBuilder().setDisconnected(value));
  }

}
