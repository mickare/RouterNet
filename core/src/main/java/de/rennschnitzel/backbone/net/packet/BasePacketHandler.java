package de.rennschnitzel.backbone.net.packet;

import java.util.UUID;

import de.rennschnitzel.backbone.ProtocolUtils;
import de.rennschnitzel.backbone.exception.ProtocolException;
import de.rennschnitzel.backbone.net.Connection;
import de.rennschnitzel.backbone.net.channel.Channel;
import de.rennschnitzel.backbone.net.protocol.LoginProtocol.*;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.*;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ChannelMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ChannelRegister;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureMessage;

public class BasePacketHandler<C extends Connection> implements PacketHandler<C> {

  public boolean isReceiver(C con, TransportProtocol.TargetMessage target) {
    return con.getNetwork().getHome().isPart(target);
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
    Channel channel = con.getChannelIfPresent(msg.getChannelId());
    if (channel != null && !channel.isClosed()) {
      channel.receiveProto(msg);
    }
  }

  @Override
  public void handle(C con, CloseMessage msg) throws Exception {
    con.remoteClosed(msg);
  }

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

}
