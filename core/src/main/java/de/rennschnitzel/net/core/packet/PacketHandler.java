package de.rennschnitzel.net.core.packet;

import de.rennschnitzel.net.exception.ProtocolException;
import de.rennschnitzel.net.protocol.LoginProtocol.*;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeRemoveMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeTopologyMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeUpdateMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ChannelMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ChannelRegister;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureMessage;

public interface PacketHandler<C> {

  void contextActive(C ctx) throws Exception;

  void handlerAdded(C ctx) throws Exception;

  default void handle(C ctx, Packet packet) throws Exception {
    switch (packet.getValueCase()) {
      // Transport
      case CLOSE:
        handle(ctx, packet.getClose());
        break;

      // Login
      case LOGINHANDSHAKE:
        handle(ctx, packet.getLoginHandshake());
        break;
      case LOGINCHALLENGE:
        handle(ctx, packet.getLoginChallenge());
        break;
      case LOGINRESPONSE:
        handle(ctx, packet.getLoginResponse());
        break;
      case LOGINSUCCESS:
        handle(ctx, packet.getLoginSuccess());
        break;
      case LOGINUPGRADE:
        handle(ctx, packet.getLoginUpgrade());
        break;

      // Network

      case NODETOPOLOGY:
        handle(ctx, packet.getNodeTopology());
        break;
      case NODEUPDATE:
        handle(ctx, packet.getNodeUpdate());
        break;
      case NODEREMOVE:
        handle(ctx, packet.getNodeRemove());
        break;

      // Channel
      case CHANNELMESSAGE:
        handle(ctx, packet.getChannelMessage());
        break;
      case CHANNELREGISTER:
        handle(ctx, packet.getChannelRegister());
        break;

      // Procedure
      case PROCEDUREMESSAGE:
        handle(ctx, packet.getProcedureMessage());
        break;
      default:
        handleUndefined(ctx, packet);
    }
  }

  default void handleUndefined(C ctx, Packet packet) throws Exception {
    throw new ProtocolException("Invalid or unknown packet!");
  }

  // Transport
  void handle(C ctx, CloseMessage msg) throws Exception;

  // Login
  void handle(C ctx, LoginHandshakeMessage msg) throws Exception;

  void handle(C ctx, LoginResponseMessage msg) throws Exception;

  void handle(C ctx, LoginChallengeMessage msg) throws Exception;

  void handle(C ctx, LoginSuccessMessage msg) throws Exception;

  void handle(C ctx, LoginUpgradeMessage msg) throws Exception;


  // Network
  void handle(C ctx, NodeTopologyMessage msg) throws Exception;

  void handle(C ctx, NodeUpdateMessage msg) throws Exception;

  void handle(C ctx, NodeRemoveMessage msg) throws Exception;

  // Channel
  void handle(C ctx, ChannelMessage msg) throws Exception;

  void handle(C ctx, ChannelRegister msg) throws Exception;

  // Procedure
  void handle(C ctx, ProcedureMessage msg) throws Exception;



}
