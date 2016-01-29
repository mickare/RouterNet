package de.rennschnitzel.net.netty;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.login.LoginEngine;
import de.rennschnitzel.net.core.login.LoginEngine.State;
import de.rennschnitzel.net.exception.ConnectionException;
import de.rennschnitzel.net.exception.HandshakeException;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ErrorMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class LoginHandler extends SimpleChannelInboundHandler<Packet> {

  private static int counter = 0;

  private final LoginEngine engine;
  int id = ++counter;

  public LoginHandler(LoginEngine engine) {
    super(Packet.class);
    Preconditions.checkNotNull(engine);
    this.engine = engine;
  }

  public Logger getLogger() {
    return engine.getNetwork().getLogger();
  }

  @Override
  public void handlerAdded(final ChannelHandlerContext ctx) throws Exception {
    this.engine.setChannel(new ChannelWrapper(ctx.channel()));
    this.engine.checkState(State.NEW);
    this.engine.getLoginFuture().addListener(f -> {
      if (f.isSuccess()) {
        ctx.fireUserEventTriggered(engine.newLoginSuccessEvent());
        ctx.pipeline().remove(LoginHandler.this);
      } else {
        Throwable cause = f.cause();
        getLogger().log(Level.WARNING, "Failed login (" + engine.getFailureState() + "): " + cause.getMessage(), cause);
        ctx.writeAndFlush(Packet.newBuilder().setClose(createCloseMessage(cause)).build()).addListener(ChannelFutureListener.CLOSE);
      }
    });
  }

  @Override
  public void channelActive(final ChannelHandlerContext ctx) throws Exception {
    this.engine.start();
    super.channelActive(ctx);
  }

  @Override
  public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
    ctx.read();
    this.engine.fail(new HandshakeException("channel inactive"));
  }

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx, final Packet msg) throws Exception {
    engine.handle(engine, msg);
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
    try {
      engine.fail(cause);
      ctx.writeAndFlush(Packet.newBuilder().setClose(createCloseMessage(cause)).build()).addListener(ChannelFutureListener.CLOSE);
    } finally {
      getLogger().log(Level.WARNING, "Exception while login (" + engine.getFailureCause() + "): " + cause.getMessage(), cause);
    }
  }

  private CloseMessage createCloseMessage(Throwable cause) {
    ErrorMessage.Type type;
    String text;
    if (cause instanceof ConnectionException) {
      ConnectionException con = (ConnectionException) cause;
      type = con.getType();
      text = con.getMessage();
    } else {
      type = ErrorMessage.Type.HANDSHAKE;
      text = "Exception while Login";
    }
    ErrorMessage.Builder error = ErrorMessage.newBuilder().setType(type).setMessage(text);
    return CloseMessage.newBuilder().setError(error).build();
  }

}
