package de.rennschnitzel.backbone.net.packet;

import de.rennschnitzel.backbone.net.protocol.LoginProtocol.LoginChallengeMessage;
import de.rennschnitzel.backbone.net.protocol.LoginProtocol.LoginHandshakeMessage;
import de.rennschnitzel.backbone.net.protocol.LoginProtocol.LoginResponseMessage;
import de.rennschnitzel.backbone.net.protocol.LoginProtocol.LoginSuccessMessage;
import de.rennschnitzel.backbone.net.protocol.LoginProtocol.LoginUpgradeMessage;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.NodeRemoveMessage;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.NodeTopologyMessage;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.NodeUpdateMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ChannelMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ChannelRegister;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureMessage;

public class SimplePacketHandler<C> implements PacketHandler<C> {

  public SimplePacketHandler() {}

  @Override
  public void handle(C ctx, CloseMessage msg) throws Exception {

  }

  @Override
  public void handle(C ctx, LoginHandshakeMessage msg) throws Exception {
    throw new UnsupportedOperationException("Not implemented!");
  }

  @Override
  public void handle(C ctx, LoginResponseMessage msg) throws Exception {
    throw new UnsupportedOperationException("Not implemented!");
  }

  @Override
  public void handle(C ctx, LoginChallengeMessage msg) throws Exception {
    throw new UnsupportedOperationException("Not implemented!");
  }

  @Override
  public void handle(C ctx, LoginSuccessMessage msg) throws Exception {
    throw new UnsupportedOperationException("Not implemented!");
  }

  @Override
  public void handle(C ctx, LoginUpgradeMessage msg) throws Exception {
    throw new UnsupportedOperationException("Not implemented!");
  }

  @Override
  public void handle(C ctx, NodeTopologyMessage msg) throws Exception {
    throw new UnsupportedOperationException("Not implemented!");
  }

  @Override
  public void handle(C ctx, NodeUpdateMessage msg) throws Exception {
    throw new UnsupportedOperationException("Not implemented!");
  }

  @Override
  public void handle(C ctx, NodeRemoveMessage msg) throws Exception {
    throw new UnsupportedOperationException("Not implemented!");
  }

  @Override
  public void handle(C ctx, ChannelMessage msg) throws Exception {
    throw new UnsupportedOperationException("Not implemented!");
  }

  @Override
  public void handle(C ctx, ChannelRegister msg) throws Exception {
    throw new UnsupportedOperationException("Not implemented!");
  }

  @Override
  public void handle(C ctx, ProcedureMessage msg) throws Exception {
    throw new UnsupportedOperationException("Not implemented!");
  }

}
