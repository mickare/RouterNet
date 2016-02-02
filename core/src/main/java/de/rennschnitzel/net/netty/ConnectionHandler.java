package de.rennschnitzel.net.netty;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.packet.PacketHandler;
import de.rennschnitzel.net.event.LoginSuccessEvent;
import de.rennschnitzel.net.event.LoginSuccessEvent.ClientLoginSuccessEvent;
import de.rennschnitzel.net.event.LoginSuccessEvent.RouterLoginSuccessEvent;
import de.rennschnitzel.net.exception.ConnectionException;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ErrorMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.Getter;

public class ConnectionHandler extends SimpleChannelInboundHandler<Packet> {

  
  private @Getter final AbstractNetwork network;
  private final PacketHandler<Connection> handler;  
  private @Getter Connection connection = null;

  public ConnectionHandler(AbstractNetwork network, PacketHandler<Connection> handler) {
    super(Packet.class);
    Preconditions.checkNotNull(network);
    this.network = network;
    this.handler = handler;
  }


  private void log(Object msg) {
    log(msg != null ? msg.toString() : "null");
  }

  private void log(String msg) {
    getLogger().info(msg);
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Packet msg) throws Exception {
    log(msg);
    long start = System.currentTimeMillis();
    this.handler.handle(connection, msg);
    log("channelRead0: " + (System.currentTimeMillis() - start));
  }

  @Override
  public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
    if (connection != null) {
      connection.removeFromNetwork();
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    try {
      ErrorMessage.Type type;
      String text;
      if (cause instanceof ConnectionException) {
        ConnectionException con = (ConnectionException) cause;
        type = con.getType();
        text = con.getMessage();
      } else {
        type = ErrorMessage.Type.SERVER_ERROR;
        text = cause.getMessage();
      }
      if (text == null) {
        text = "null";
      }
      ErrorMessage.Builder error = ErrorMessage.newBuilder().setType(type).setMessage(text);

      ctx.writeAndFlush(CloseMessage.newBuilder().setError(error)).addListener(ChannelFutureListener.CLOSE);
    } finally {
      getLogger().log(Level.WARNING, "Channel Exception: " + cause.getMessage(), cause);
    }
  }

  public Logger getLogger() {
    return network.getLogger();
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

    log(evt);

    if (evt instanceof LoginSuccessEvent) {

      LoginSuccessEvent login = (LoginSuccessEvent) evt;
      this.connection = login.getConnection();
      this.connection.addToNetwork();

      if (login instanceof ClientLoginSuccessEvent) {
        network.updateNode(((ClientLoginSuccessEvent) login).getNodeMessage());
      } else if (login instanceof RouterLoginSuccessEvent) {
        network.updateNodes(((RouterLoginSuccessEvent) login).getNodeTopology());
      }
    }

  }

}
