package de.mickare.metricweb.websocket;

import java.util.logging.Level;

import de.mickare.metricweb.protocol.WebProtocol.PacketMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public @RequiredArgsConstructor class WebSocketConnectionHandler
    extends SimpleChannelInboundHandler<PacketMessage> {

  private @NonNull final WebSocketServer server;
  private WebConnection connection;

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, PacketMessage msg) throws Exception {
    this.connection.handle(msg);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    this.connection = server.newConnection(ctx.channel());
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    server.getLogger().log(Level.WARNING, "Channel Exception", cause);
    ctx.channel().close();
  }

}
