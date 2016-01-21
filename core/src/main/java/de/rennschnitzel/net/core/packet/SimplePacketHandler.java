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
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.TunnelMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.TunnelRegister;

public class SimplePacketHandler<C> implements PacketHandler<C> {

  public SimplePacketHandler() {}

  @Override
  public void handlerAdded(C ctx) throws Exception {}

  @Override
  public void contextActive(C ctx) throws Exception {}

  @Override
  public void handle(C ctx, CloseMessage msg) throws Exception {
    throw new UnsupportedOperationException("Not implemented!");
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
  public void handle(C ctx, TunnelMessage msg) throws Exception {
    throw new UnsupportedOperationException("Not implemented!");
  }

  @Override
  public void handle(C ctx, TunnelRegister msg) throws Exception {
    throw new UnsupportedOperationException("Not implemented!");
  }

  @Override
  public void handle(C ctx, ProcedureMessage msg) throws Exception {
    throw new UnsupportedOperationException("Not implemented!");
  }

}
