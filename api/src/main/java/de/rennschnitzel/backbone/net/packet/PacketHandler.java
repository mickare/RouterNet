package de.rennschnitzel.backbone.net.packet;

import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.AuthChallengeMessage;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.AuthResponseMessage;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.LoginMessage;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.ConnectedMessage;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.DisconnectedMessage;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.ServerUpdateMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ChannelMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ChannelRegister;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ErrorMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.Packet;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureMessage;
import de.rennschnitzel.backbone.netty.exception.ProtocolException;

public interface PacketHandler<C> {

  default void handle(C ctx, Packet packet) throws Exception {
    switch (packet.getValueCase()) {
      case ERROR:
        handle(ctx, packet.getError());
        break;
      case CLOSE:
        handle(ctx, packet.getClose());
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
      case CONNECTED:
        handle(ctx, packet.getConnected());
        break;
      case DISCONNECTED:
        handle(ctx, packet.getDisconnected());
        break;
      case UPDATE:
        handle(ctx, packet.getUpdate());
        break;
      case CHANNELMESSAGE:
        handle(ctx, packet.getChannelMessage());
        break;
      case CHANNELREGISTER:
        handle(ctx, packet.getChannelRegister());
        break;
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

  void handle(C ctx, ProcedureMessage msg) throws Exception;

  void handle(C ctx, ChannelRegister msg) throws Exception;

  void handle(C ctx, ChannelMessage msg) throws Exception;

  void handle(C ctx, ServerUpdateMessage msg) throws Exception;

  void handle(C ctx, DisconnectedMessage msg) throws Exception;

  void handle(C ctx, ConnectedMessage msg) throws Exception;

  void handle(C ctx, AuthResponseMessage msg) throws Exception;

  void handle(C ctx, AuthChallengeMessage msg) throws Exception;

  void handle(C ctx, LoginMessage msg) throws Exception;

  void handle(C ctx, CloseMessage msg) throws Exception;

  void handle(C ctx, ErrorMessage msg) throws Exception;

}
