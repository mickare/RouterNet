package de.rennschnitzel.net.netty;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.login.LoginHandler;
import de.rennschnitzel.net.core.packet.PacketHandler;
import de.rennschnitzel.net.exception.ConnectionException;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ErrorMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.AccessLevel;
import lombok.Getter;

public class MainHandler<N extends AbstractNetwork> extends SimpleChannelInboundHandler<Packet> {

  private static enum State {
    NEW, LOGIN, RUNNING, CLOSED;
  }

  private final N network;

  @Getter(AccessLevel.PROTECTED)
  private ChannelHandlerContext context;

  @Getter
  private volatile State state = State.NEW;

  private LoginHandler<ChannelHandlerContext> loginHandler = null;
  private final PacketHandler<NettyConnection<N>> packetHandler;
  private NettyConnection<N> connection = null;
  private String name = "unknown";

  public MainHandler(N network, LoginHandler<ChannelHandlerContext> loginHandler,
      PacketHandler<NettyConnection<N>> packetHandler) {
    super(Packet.class, true);

    Preconditions.checkNotNull(network);
    Preconditions.checkNotNull(loginHandler);
    Preconditions.checkNotNull(packetHandler);

    this.network = network;
    this.loginHandler = loginHandler;
    this.packetHandler = packetHandler;
  }

  private void debug(String msg) {
    getLogger().info(msg);
  }

  public Logger getLogger() {
    return network.getLogger();
  }

  public ChannelFuture send(Packet packet) {
    ChannelFuture future = this.context.write(packet);
    future.addListener(f -> {
      if (!f.isSuccess()) {
        getLogger().log(Level.SEVERE, "send " + packet, f.cause());
      } else {
        debug(packet.toString());
      }
    });
    return future;

  }

  public ChannelFuture sendAndFlush(Packet packet) {
    ChannelFuture f = this.context.writeAndFlush(packet);
    f.addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        if (!future.isSuccess()) {
          getLogger().log(Level.SEVERE, "sendAndFlush " + packet, future.cause());
        }
      }
    });
    return f;
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    debug("handlerAdded 1, " + ctx);
    Preconditions.checkState(this.state == State.NEW);

    this.state = State.LOGIN;
    this.context = ctx;

    loginHandler.handlerAdded(ctx);

    debug("handlerAdded 2, " + ctx);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    debug("channelActive 1, " + ctx);
    Preconditions.checkState(this.state == State.LOGIN);

    loginHandler.channelActive(ctx);

    debug("channelActive 2, " + ctx);
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


    if (packet.getValueCase() == Packet.ValueCase.CLOSE) {

      getLogger().log(Level.INFO, "Connection closed:\n" + packet.getClose());

      ctx.channel().close();
      this.state = State.CLOSED;
      return;
    }

    //debug(packet.toString());

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
          this.loginHandler.getConnectionFuture().set(connection);

        } else {

          this.state = State.CLOSED;
        }
      }
    } else if (state == State.RUNNING) {
      connection.receive(packet);
    } else {
      throw new IllegalStateException();
    }

    debug("state: " + state);
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
    try {
      this.loginHandler.getConnectionFuture().setException(cause);
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
