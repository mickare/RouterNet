package de.rennschnitzel.net.core.packet;

import de.rennschnitzel.net.protocol.LoginProtocol.LoginChallengeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginHandshakeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginResponseMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginSuccessMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginUpgradeMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeRemoveMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeTopologyMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeUpdateMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.HeartbeatMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import de.rennschnitzel.net.protocol.TransportProtocol.TunnelMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.TunnelRegister;
import io.netty.util.concurrent.Future;


public interface PacketWriterPromise<F extends Future<?>> {

  void writeFast(Packet packet);

  default void writeFast(Packet.Builder builder) {
     writeFast(builder.build());
  }

  void flush();

  void writeAndFlushFast(Packet packet);

  default void writeAndFlushFast(Packet.Builder builder) {
     writeAndFlushFast(builder.build());
  }


  // ******************************************************************************
  // Transport

  default void writeFast(CloseMessage.Builder builder) {
     writeFast(builder.build());
  }

  default void writeFast(CloseMessage value) {
     writeFast(Packer.pack(value));
  }

  default void writeAndFlushFast(CloseMessage.Builder builder) {
     writeAndFlushFast(builder.build());
  }

  default void writeAndFlushFast(CloseMessage value) {
     writeAndFlushFast(Packer.pack(value));
  }



  default void writeFast(HeartbeatMessage.Builder builder) {
     writeFast(builder.build());
  }

  default void writeFast(HeartbeatMessage value) {
     writeFast(Packer.pack(value));
  }

  default void writeAndFlushFast(HeartbeatMessage.Builder builder) {
     writeAndFlushFast(builder.build());
  }

  default void writeAndFlushFast(HeartbeatMessage value) {
     writeAndFlushFast(Packer.pack(value));
  }

  // ******************************************************************************
  // Handshake

  default void writeFast(LoginHandshakeMessage.Builder builder) {
     writeFast(builder.build());
  }

  default void writeFast(LoginHandshakeMessage value) {
     writeFast(Packer.pack(value));
  }

  default void writeAndFlushFast(LoginHandshakeMessage.Builder builder) {
     writeAndFlushFast(builder.build());
  }

  default void writeAndFlushFast(LoginHandshakeMessage value) {
     writeAndFlushFast(Packer.pack(value));
  }



  default void writeFast(LoginChallengeMessage.Builder builder) {
     writeFast(builder.build());
  }

  default void writeFast(LoginChallengeMessage value) {
     writeFast(Packer.pack(value));
  }

  default void writeAndFlushFast(LoginChallengeMessage.Builder builder) {
     writeAndFlushFast(builder.build());
  }

  default void writeAndFlushFast(LoginChallengeMessage value) {
     writeAndFlushFast(Packer.pack(value));
  }



  default void writeFast(LoginResponseMessage.Builder builder) {
     writeFast(builder.build());
  }

  default void writeFast(LoginResponseMessage value) {
     writeFast(Packer.pack(value));
  }

  default void writeAndFlushFast(LoginResponseMessage.Builder builder) {
     writeAndFlushFast(builder.build());
  }

  default void writeAndFlushFast(LoginResponseMessage value) {
     writeAndFlushFast(Packer.pack(value));
  }



  default void writeFast(LoginSuccessMessage.Builder builder) {
     writeFast(builder.build());
  }

  default void writeFast(LoginSuccessMessage value) {
     writeFast(Packer.pack(value));
  }

  default void writeAndFlushFast(LoginSuccessMessage.Builder builder) {
     writeAndFlushFast(builder.build());
  }

  default void writeAndFlushFast(LoginSuccessMessage value) {
     writeAndFlushFast(Packer.pack(value));
  }



  default void writeFast(LoginUpgradeMessage.Builder builder) {
     writeFast(builder.build());
  }

  default void writeFast(LoginUpgradeMessage value) {
     writeFast(Packer.pack(value));
  }

  default void writeAndFlushFast(LoginUpgradeMessage.Builder builder) {
     writeAndFlushFast(builder.build());
  }

  default void writeAndFlushFast(LoginUpgradeMessage value) {
     writeAndFlushFast(Packer.pack(value));
  }



  // ******************************************************************************
  // Network

  default void writeFast(NodeTopologyMessage.Builder builder) {
     writeFast(builder.build());
  }

  default void writeFast(NodeTopologyMessage value) {
     writeFast(Packer.pack(value));
  }

  default void writeAndFlushFast(NodeTopologyMessage.Builder builder) {
     writeAndFlushFast(builder.build());
  }

  default void writeAndFlushFast(NodeTopologyMessage value) {
     writeAndFlushFast(Packer.pack(value));
  }



  default void writeFast(NodeUpdateMessage.Builder builder) {
     writeFast(builder.build());
  }

  default void writeFast(NodeUpdateMessage value) {
     writeFast(Packer.pack(value));
  }

  default void writeAndFlushFast(NodeUpdateMessage.Builder builder) {
     writeAndFlushFast(builder.build());
  }

  default void writeAndFlushFast(NodeUpdateMessage value) {
     writeAndFlushFast(Packer.pack(value));
  }



  default void writeFast(NodeRemoveMessage.Builder builder) {
     writeFast(builder.build());
  }

  default void writeFast(NodeRemoveMessage value) {
     writeFast(Packer.pack(value));
  }

  default void writeAndFlushFast(NodeRemoveMessage.Builder builder) {
     writeAndFlushFast(builder.build());
  }

  default void writeAndFlushFast(NodeRemoveMessage value) {
     writeAndFlushFast(Packer.pack(value));
  }


  // ******************************************************************************
  // Tunnel

  default void writeFast(TunnelMessage.Builder builder) {
     writeFast(builder.build());
  }

  default void writeFast(TunnelMessage value) {
     writeFast(Packer.pack(value));
  }

  default void writeAndFlushFast(TunnelMessage.Builder builder) {
     writeAndFlushFast(builder.build());
  }

  default void writeAndFlushFast(TunnelMessage value) {
     writeAndFlushFast(Packer.pack(value));
  }



  default void writeFast(TunnelRegister.Builder builder) {
     writeFast(builder.build());
  }

  default void writeFast(TunnelRegister value) {
     writeFast(Packer.pack(value));
  }

  default void writeAndFlushFast(TunnelRegister.Builder builder) {
     writeAndFlushFast(builder.build());
  }

  default void writeAndFlushFast(TunnelRegister value) {
     writeAndFlushFast(Packer.pack(value));
  }


}
