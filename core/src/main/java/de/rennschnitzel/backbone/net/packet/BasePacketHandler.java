package de.rennschnitzel.backbone.net.packet;

import de.rennschnitzel.backbone.exception.ProtocolException;
import de.rennschnitzel.backbone.net.Connection;
import de.rennschnitzel.backbone.net.channel.Channel;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.AuthChallengeMessage;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.AuthResponseMessage;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.AuthSuccessMessage;
import de.rennschnitzel.backbone.net.protocol.HandshakeProtocol.LoginMessage;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.ConnectedMessage;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.DisconnectedMessage;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.ServerUpdateMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ChannelMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ChannelRegister;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureMessage;

public class BasePacketHandler<C extends Connection> implements PacketHandler<C> {

  public boolean isReceiver(C con, TransportProtocol.TargetMessage target) {
    return con.getHome().isPart(target);
  }

  @Override
  public void handle(C con, ProcedureMessage msg) throws Exception {
    if (!isReceiver(con, msg.getTarget())) {
      return; // drop packet
    }
    con.getNetwork().getProcedureManager().handle(msg);
  }

  @Override
  public void handle(C con, ChannelRegister msg) throws Exception {
    con.registerChannel(msg);
  }

  @Override
  public void handle(C con, ChannelMessage msg) throws Exception {
    if (!isReceiver(con, msg.getTarget())) {
      return; // drop packet
    }
    Channel channel = con.getChannel(msg.getChannelId());
    if (channel != null && !channel.isClosed()) {
      channel.receiveProto(msg);
    }
  }

  @Override
  public void handle(C con, ServerUpdateMessage msg) throws Exception {
    con.getNetwork().updateNodes(msg);
  }

  @Override
  public void handle(C con, DisconnectedMessage msg) throws Exception {
    con.getNetwork().updateNodes(msg);
  }

  @Override
  public void handle(C con, ConnectedMessage msg) throws Exception {
    con.getNetwork().updateNodes(msg);
  }

  @Override
  public void handle(C con, AuthResponseMessage msg) throws Exception {
    throw new ProtocolException("Invalid packet!");
  }

  @Override
  public void handle(C con, AuthChallengeMessage msg) throws Exception {
    throw new ProtocolException("Invalid packet!");
  }

  @Override
  public void handle(C con, LoginMessage msg) throws Exception {
    throw new ProtocolException("Invalid packet!");
  }

  @Override
  public void handle(C con, CloseMessage msg) throws Exception {
    throw new ProtocolException("Invalid packet!");
  }

  @Override
  public void handle(C ctx, AuthSuccessMessage msg) throws Exception {
    throw new ProtocolException("Invalid packet!");
  }

}
