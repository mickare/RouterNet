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

public class BasePacketHandler<C extends Connection> implements PacketHandler<C> {

  @Override
  public void handlerAdded(C ctx) throws Exception {}

  @Override
  public void channelActive(C ctx) throws Exception {}

  @Override
  public void channelInactive(C ctx) throws Exception {}


  public boolean isReceiver(C con, TransportProtocol.TargetMessage target) {
    return con.getNetwork().getHome().isPart(target);
  }


  // TRANSPORT
  
  @Override
  public void handle(C con, CloseMessage msg) throws Exception {
    con.setCloseMessage(msg);
  }

  @Override
  public void handle(C ctx, HeartbeatMessage heartbeat) throws Exception {

  }
  

  // LOGIN

  @Override
  public void handle(C con, LoginHandshakeMessage msg) throws Exception {
    throw new ProtocolException("Invalid packet!");
  }

  @Override
  public void handle(C con, LoginResponseMessage msg) throws Exception {
    throw new ProtocolException("Invalid packet!");
  }

  @Override
  public void handle(C con, LoginChallengeMessage msg) throws Exception {
    throw new ProtocolException("Invalid packet!");
  }

  @Override
  public void handle(C con, LoginSuccessMessage msg) throws Exception {
    throw new ProtocolException("Invalid packet!");
  }

  @Override
  public void handle(C con, LoginUpgradeMessage msg) throws Exception {
    throw new ProtocolException("Invalid packet!");
  }


  // NETWORK

  @Override
  public void handle(C con, NodeTopologyMessage msg) throws Exception {
    con.getNetwork().updateNodes(msg);
  }

  @Override
  public void handle(C con, NodeUpdateMessage msg) throws Exception {
    con.getNetwork().updateNode(msg.getNode());
  }

  @Override
  public void handle(C con, NodeRemoveMessage msg) throws Exception {
    UUID id = ProtocolUtils.convert(msg.getId());
    con.getNetwork().removeNode(id);
  }
  
  
  // TUNNEL
  
  @Override
  public void handle(C con, TunnelRegister msg) throws Exception {
    con.registerTunnel(msg);
  }

  @Override
  public void handle(C con, TunnelMessage msg) throws Exception {
    if (!isReceiver(con, msg.getTarget())) {
      return; // drop packet
    }
    String channelName = con.getTunnelNameIfPresent(msg.getTunnelId());
    if (channelName != null) {
      Tunnel channel = con.getNetwork().getTunnelIfPresent(channelName);
      if (channel != null && !channel.isClosed()) {
        channel.receiveProto(msg);
      }
    }
  }

  
  // PROCEDURE
  
  @Override
  public void handle(C con, ProcedureMessage msg) throws Exception {
    if (!isReceiver(con, msg.getTarget())) {
      return; // drop packet
    }
    con.getNetwork().getProcedureManager().handle(con, msg);
  }




}
