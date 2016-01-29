package de.rennschnitzel.net.netty;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.login.LoginEngine;
import de.rennschnitzel.net.core.packet.PacketHandler;
import de.rennschnitzel.net.exception.ConnectionException;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ErrorMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.HeartbeatMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.AccessLevel;
import lombok.Getter;

public class MainHandler<N extends AbstractNetwork> extends SimpleChannelInboundHandler<Packet> {

  private static enum State {
    NEW, LOGIN, RUNNING, CLOSED;
  }

  @Getter
  private final String handlerName = "main";

  private final N network;

  @Getter(AccessLevel.PROTECTED)
  private ChannelHandlerContext context;

  @Getter
  private volatile State state = State.NEW;

  @Getter
  private final LoginEngine<ChannelHandlerContext> loginHandler;
  private IdleStateHandler idleHandler = new IdleStateHandler(10, 5, 0);

  @Getter
  private final PacketHandler<NettyConnection<N>> packetHandler;
  @Getter
  private NettyConnection<N> connection = null;
  private String name = "unknown";



  public MainHandler(N network, LoginEngine<ChannelHandlerContext> loginHandler,
      PacketHandler<NettyConnection<N>> packetHandler) {
    super(Packet.class, true);

    Preconditions.checkNotNull(network);
    Preconditions.checkNotNull(loginHandler);
    Preconditions.checkNotNull(packetHandler);

    this.network = network;
    this.loginHandler = loginHandler;
    this.packetHandler = packetHandler;
  }

  public Logger getLogger() {
    return network.getLogger();
  }

  public ChannelFuture send(Packet.Builder packet) {
    return send(packet.build());
  }

  public ChannelFuture send(Packet packet) {
    return this.context.write(packet);

  }

  public ChannelFuture sendAndFlush(Packet.Builder packet) {
    return sendAndFlush(packet.build());
  }

  public ChannelFuture sendAndFlush(Packet packet) {
    return this.context.writeAndFlush(packet);
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    Preconditions.checkState(this.state == State.NEW);

    this.state = State.LOGIN;
    this.context = ctx;

    ctx.pipeline().addBefore(this.getHandlerName(), name, idleHandler);
    loginHandler.handlerAdded(ctx);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    Preconditions.checkState(this.state == State.LOGIN);
    loginHandler.channelActive(ctx);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {

    if (this.state == State.LOGIN && !loginHandler.isDone()) {
      loginHandler.fail(ctx,
          new ConnectionException(ErrorMessage.Type.UNDEFINED, "channel inactive"));
    }

    getLogger().info("closed " + ctx.channel().toString());
    this.state = State.CLOSED;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {

    // debug(packet.toString());

    if (state == State.LOGIN) {
      loginHandler.handle(ctx, packet);
      if (loginHandler.isDone()) {
        if (loginHandler.isSuccess()) {
          this.state = State.RUNNING;
          this.name = this.loginHandler.getName();
          this.connection =
              new NettyConnection<N>(network, loginHandler.getId(), this, this.packetHandler);
          this.loginHandler.registerNodes();
          this.network.addConnection(connection);
          this.packetHandler.channelActive(connection);
          this.loginHandler.getConnectionPromise().setSuccess(connection);

        } else {

          this.state = State.CLOSED;
        }
      }
    } else if (state == State.RUNNING) {
      connection.receive(packet);
    } else {
      throw new IllegalStateException();
    }

  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

    if (evt instanceof IdleStateEvent) {
      IdleStateEvent e = (IdleStateEvent) evt;
      if (state == State.LOGIN) {
        if (e.state() == IdleState.READER_IDLE) {
          disconnect(ctx, ErrorMessage.Type.TIMEOUT, "read timeout");
        } else if (e.state() == IdleState.WRITER_IDLE) {
          disconnect(ctx, ErrorMessage.Type.TIMEOUT, "write timeout");
        }
      } else if (this.state == State.RUNNING) {
        if (e.state() == IdleState.READER_IDLE) {
          disconnect(ctx, ErrorMessage.Type.TIMEOUT, "read timeout");
        } else if (e.state() == IdleState.WRITER_IDLE) {
          this.sendAndFlush(Packet.newBuilder().setHeartbeat(HeartbeatMessage.newBuilder()));
        }
      }
    }
  }


  public ChannelFuture disconnect(final ChannelHandlerContext ctx, String reason) throws Exception {
    return disconnect(ctx, CloseMessage.newBuilder().setNormal(reason).build());
  }

  public ChannelFuture disconnect(final ChannelHandlerContext ctx, ErrorMessage.Type type,
      String msg) throws Exception {
    return disconnect(ctx, ErrorMessage.newBuilder().setType(type).setMessage(msg).build());
  }

  public ChannelFuture disconnect(final ChannelHandlerContext ctx, ErrorMessage msg)
      throws Exception {
    return disconnect(ctx, CloseMessage.newBuilder().setError(msg).build());
  }

  public ChannelFuture disconnect(final ChannelHandlerContext ctx, final CloseMessage msg)
      throws Exception {
    ChannelFuture f = PacketUtil.writeAndFlush(ctx.channel(), msg);
    f.addListener(ChannelFutureListener.CLOSE);
    return f;
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
    try {
      if (this.loginHandler.fail(ctx, cause)) {
        ctx.close();
        return;
      }
      getLogger().log(Level.WARNING, this.name + " lost connection", cause);

      ErrorMessage.Type type;
      String text;
      if (cause instanceof ConnectionException) {
        ConnectionException con = (ConnectionException) cause;
        type = con.getType();
        text = con.getMessage();
        if (con.isDoLog()) {
          getLogger().log(Level.INFO, type.name() + " " + con.getMessage(), con);
        }
      } else {
        type = ErrorMessage.Type.SERVER_ERROR;
        text = "Server Error";
      }
      ErrorMessage.Builder error = ErrorMessage.newBuilder().setType(type).setMessage(text);

      ChannelFuture f =
          PacketUtil.writeAndFlush(ctx.channel(), CloseMessage.newBuilder().setError(error));
      f.addListener(ChannelFutureListener.CLOSE);


    } catch (final Exception ex) {
      getLogger().log(Level.SEVERE,
          "ERROR trying to close socket because we got an unhandled exception", ex);
      ctx.close();
    }
  }
}
