package de.rennschnitzel.backbone.net.packet;

import de.rennschnitzel.backbone.net.protocol.DataStoreProtocol.DataStoreRequestMessage;
import de.rennschnitzel.backbone.net.protocol.DataStoreProtocol.DataStoreResponseMessage;
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
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureMessage;

public class SimplePacketHandler<C> implements PacketHandler<C> {

  public SimplePacketHandler() {}

  @Override
  public void handle(C ctx, DataStoreResponseMessage msg) throws Exception {
    throw new UnsupportedOperationException("Not implemented!");
  }

  @Override
  public void handle(C ctx, DataStoreRequestMessage msg) throws Exception {
    throw new UnsupportedOperationException("Not implemented!");
  }

  @Override
  public void handle(C ctx, ProcedureMessage msg) throws Exception {
    throw new UnsupportedOperationException("Not implemented!");
  }

  @Override
  public void handle(C ctx, ChannelRegister msg) throws Exception {
    throw new UnsupportedOperationException("Not implemented!");
  }

  @Override
  public void handle(C ctx, ChannelMessage msg) throws Exception {
    throw new UnsupportedOperationException("Not implemented!");
  }

  @Override
  public void handle(C ctx, ServerUpdateMessage msg) throws Exception {
    throw new UnsupportedOperationException("Not implemented!");
  }

  @Override
  public void handle(C ctx, DisconnectedMessage msg) throws Exception {
    throw new UnsupportedOperationException("Not implemented!");
  }

  @Override
  public void handle(C ctx, ConnectedMessage msg) throws Exception {
    throw new UnsupportedOperationException("Not implemented!");
  }

  @Override
  public void handle(C ctx, AuthResponseMessage msg) throws Exception {
    throw new UnsupportedOperationException("Not implemented!");
  }

  @Override
  public void handle(C ctx, AuthChallengeMessage msg) throws Exception {
    throw new UnsupportedOperationException("Not implemented!");
  }

  @Override
  public void handle(C ctx, LoginMessage msg) throws Exception {
    throw new UnsupportedOperationException("Not implemented!");
  }

  @Override
  public void handle(C ctx, CloseMessage msg) throws Exception {
    throw new UnsupportedOperationException("Not implemented!");
  }

  @Override
  public void handle(C ctx, ErrorMessage msg) throws Exception {
    throw new UnsupportedOperationException("Not implemented!");
  }

}
