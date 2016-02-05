package de.rennschnitzel.net.core.packet;

import java.util.UUID;

import de.rennschnitzel.net.ProtocolUtils;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.Tunnel;
import de.rennschnitzel.net.exception.ProtocolException;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginChallengeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginHandshakeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginResponseMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginSuccessMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginUpgradeMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeRemoveMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeTopologyMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeUpdateMessage;
import de.rennschnitzel.net.protocol.TransportProtocol;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.HeartbeatMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.TunnelMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.TunnelRegister;

public class BasePacketHandler implements PacketHandler<Connection> {

  public static final BasePacketHandler DEFAULT = new BasePacketHandler();
  
  public boolean isReceiver(Connection con, TransportProtocol.TargetMessage target) {
    return con.getNetwork().getHome().isPart(target);
  }


  // TRANSPORT

  @Override
  public void handle(Connection con, CloseMessage msg) throws Exception {
    con.setCloseMessage(msg);
  }

  @Override
  public void handle(Connection con, HeartbeatMessage heartbeat) throws Exception {

  }


  // LOGIN

  @Override
  public void handle(Connection con, LoginHandshakeMessage msg) throws Exception {
    throw new ProtocolException("Invalid packet!");
  }

  @Override
  public void handle(Connection con, LoginResponseMessage msg) throws Exception {
    throw new ProtocolException("Invalid packet!");
  }

  @Override
  public void handle(Connection con, LoginChallengeMessage msg) throws Exception {
    throw new ProtocolException("Invalid packet!");
  }

  @Override
  public void handle(Connection con, LoginSuccessMessage msg) throws Exception {
    throw new ProtocolException("Invalid packet!");
  }

  @Override
  public void handle(Connection con, LoginUpgradeMessage msg) throws Exception {
    throw new ProtocolException("Invalid packet!");
  }


  // NETWORK

  @Override
  public void handle(Connection con, NodeTopologyMessage msg) throws Exception {
    con.getNetwork().updateNodes(msg);
  }

  @Override
  public void handle(Connection con, NodeUpdateMessage msg) throws Exception {
    con.getNetwork().updateNode(msg.getNode());
  }

  @Override
  public void handle(Connection con, NodeRemoveMessage msg) throws Exception {
    UUID id = ProtocolUtils.convert(msg.getId());
    con.getNetwork().removeNode(id);
  }


  // TUNNEL

  @Override
  public void handle(Connection con, TunnelRegister msg) throws Exception {
    con.receive(msg);
  }

  @Override
  public void handle(Connection con, TunnelMessage msg) throws Exception {
    if (!isReceiver(con, msg.getTarget())) {
      return; // drop packet
    }
    Tunnel tunnel = con.getNetwork().getTunnelById(msg.getTunnelId());
    if (tunnel != null && !tunnel.isClosed()) {
      tunnel.receiveProto(msg);
    }
  }


  // PROCEDURE

  @Override
  public void handle(Connection con, ProcedureMessage msg) throws Exception {
    con.getNetwork().getProcedureManager().handle(msg);
  }



}
