package de.rennschnitzel.net.netty;

import java.util.logging.Level;
import java.util.logging.Logger;

import de.rennschnitzel.net.protocol.LoginProtocol.LoginChallengeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginHandshakeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginResponseMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginSuccessMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginUpgradeMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeRemoveMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeTopologyMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeUpdateMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.TunnelMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

public final class PacketUtil {

  private PacketUtil() {}


  public static final ChannelFuture writeAndFlush(final Channel ch, //
      final Packet packet) {
    ChannelFuture f = ch.writeAndFlush(packet);
    f.addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        if (!future.isSuccess()) {
          Logger.getGlobal().log(Level.SEVERE, "sendAndFlush", future.cause());
        }
      }
    });
    return f;
  }


  public static final ChannelFuture writeAndFlush(final Channel ch, //
      final Packet.Builder builder) {
    return writeAndFlush(ch, builder.build());
  }

  /*
   * public static final ChannelFuture write(final Channel ch,// final TransportProto.Packet packet)
   * { return ch.write(packet); }
   * 
   * 
   * public static final ChannelFuture write(final Channel ch,// final TransportProto.Packet.Builder
   * builder) { return write(ch, builder.build()); }
   */

  // ******************************************************************************
  // Util

  public static final ChannelFuture writeAndFlush(final Channel ch, //

  final CloseMessage.Builder value) {
    return writeAndFlush(ch, Packet.newBuilder().setClose(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch, //
      final CloseMessage value) {
    return writeAndFlush(ch, Packet.newBuilder().setClose(value));
  }


  // ******************************************************************************
  // Handshake
  public static final ChannelFuture writeAndFlush(final Channel ch, //
      final LoginHandshakeMessage.Builder value) {
    return writeAndFlush(ch, Packet.newBuilder().setLoginHandshake(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch, //
      final LoginHandshakeMessage value) {
    return writeAndFlush(ch, Packet.newBuilder().setLoginHandshake(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch, //

  final LoginChallengeMessage.Builder value) {
    return writeAndFlush(ch, Packet.newBuilder().setLoginChallenge(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch, //
      final LoginChallengeMessage value) {
    return writeAndFlush(ch, Packet.newBuilder().setLoginChallenge(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch, //

  final LoginResponseMessage.Builder value) {
    return writeAndFlush(ch, Packet.newBuilder().setLoginResponse(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch, //
      final LoginResponseMessage value) {
    return writeAndFlush(ch, Packet.newBuilder().setLoginResponse(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch, //
      final LoginSuccessMessage.Builder value) {
    return writeAndFlush(ch, Packet.newBuilder().setLoginSuccess(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch, //
      final LoginSuccessMessage value) {
    return writeAndFlush(ch, Packet.newBuilder().setLoginSuccess(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch, //
      final LoginUpgradeMessage.Builder value) {
    return writeAndFlush(ch, Packet.newBuilder().setLoginUpgrade(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch, //
      final LoginUpgradeMessage value) {
    return writeAndFlush(ch, Packet.newBuilder().setLoginUpgrade(value));
  }

  // ******************************************************************************
  // Tunnel
  
  public static final ChannelFuture writeAndFlush(final Channel ch, //
      final TunnelMessage.Builder value) {
    return writeAndFlush(ch, Packet.newBuilder().setTunnelMessage(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch, //
      final TunnelMessage value) {
    return writeAndFlush(ch, Packet.newBuilder().setTunnelMessage(value));
  }


  // ******************************************************************************
  // Network

  public static final ChannelFuture writeAndFlush(final Channel ch, //
      final NodeTopologyMessage.Builder value) {
    return writeAndFlush(ch, Packet.newBuilder().setNodeTopology(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch, //
      final NodeTopologyMessage value) {
    return writeAndFlush(ch, Packet.newBuilder().setNodeTopology(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch, //
      final NodeUpdateMessage.Builder value) {
    return writeAndFlush(ch, Packet.newBuilder().setNodeUpdate(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch, //
      final NodeUpdateMessage value) {
    return writeAndFlush(ch, Packet.newBuilder().setNodeUpdate(value));
  }


  public static final ChannelFuture writeAndFlush(final Channel ch, //
      final NodeRemoveMessage.Builder value) {
    return writeAndFlush(ch, Packet.newBuilder().setNodeRemove(value));
  }

  public static final ChannelFuture writeAndFlush(final Channel ch, //
      final NodeRemoveMessage value) {
    return writeAndFlush(ch, Packet.newBuilder().setNodeRemove(value));
  }



}
