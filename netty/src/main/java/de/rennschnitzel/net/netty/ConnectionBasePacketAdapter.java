package de.rennschnitzel.net.netty;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.packet.BasePacketHandler;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginChallengeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginHandshakeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginResponseMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginSuccessMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginUpgradeMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeRemoveMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeTopologyMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeUpdateMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ChannelMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ChannelRegister;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureMessage;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConnectionBasePacketAdapter<C extends Connection> extends NettyPacketHandler {

  @Getter
  @NonNull
  private final C connection;

  @Getter
  @NonNull
  private final BasePacketHandler<C> handler;

  @Getter
  private ChannelHandlerContext context = null;

  @Override
  public synchronized void handlerAdded(ChannelHandlerContext ctx) {
    Preconditions.checkNotNull(ctx);
    Preconditions.checkState(this.context == null);
    this.context = ctx;
  }

  @Override
  public void handle(ChannelHandlerContext ctx, ProcedureMessage msg) throws Exception {
    handler.handle(connection, msg);
  }

  @Override
  public void handle(ChannelHandlerContext ctx, ChannelRegister msg) throws Exception {
    handler.handle(connection, msg);
  }

  @Override
  public void handle(ChannelHandlerContext ctx, ChannelMessage msg) throws Exception {
    handler.handle(connection, msg);
  }

  @Override
  public void handle(ChannelHandlerContext ctx, CloseMessage msg) throws Exception {
    handler.handle(connection, msg);
  }

  @Override
  public void handle(ChannelHandlerContext ctx, LoginHandshakeMessage msg) throws Exception {
    handler.handle(connection, msg);
  }

  @Override
  public void handle(ChannelHandlerContext ctx, LoginResponseMessage msg) throws Exception {
    handler.handle(connection, msg);
  }

  @Override
  public void handle(ChannelHandlerContext ctx, LoginChallengeMessage msg) throws Exception {
    handler.handle(connection, msg);
  }

  @Override
  public void handle(ChannelHandlerContext ctx, LoginSuccessMessage msg) throws Exception {
    handler.handle(connection, msg);
  }

  @Override
  public void handle(ChannelHandlerContext ctx, LoginUpgradeMessage msg) throws Exception {
    handler.handle(connection, msg);
  }

  @Override
  public void handle(ChannelHandlerContext ctx, NodeTopologyMessage msg) throws Exception {
    handler.handle(connection, msg);
  }

  @Override
  public void handle(ChannelHandlerContext ctx, NodeUpdateMessage msg) throws Exception {
    handler.handle(connection, msg);
  }

  @Override
  public void handle(ChannelHandlerContext ctx, NodeRemoveMessage msg) throws Exception {
    handler.handle(connection, msg);
  }

}
