package de.rennschnitzel.backbone.netty;

import java.util.logging.Level;
import java.util.logging.Logger;

import de.rennschnitzel.backbone.exception.ConnectionException;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ErrorMessage;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ExceptionHandler extends ChannelHandlerAdapter {

  private final Logger logger;

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
    boolean fatal = true;
    try {
      ErrorMessage.Type type;
      String text;
      if (cause instanceof ConnectionException) {
        ConnectionException con = (ConnectionException) cause;
        fatal = con.isFatal();
        type = con.getType();
        text = con.getMessage();
        if (con.isDoLog()) {
          logger.log(Level.INFO, type.name() + " " + con.getMessage() + (fatal ? " (fatal)" : ""),
              con);
        }
      } else {
        type = ErrorMessage.Type.SERVER_ERROR;
        text = "Server Error";
      }
      ErrorMessage.Builder error = ErrorMessage.newBuilder().setType(type).setMessage(text);
      if (fatal) {
        ChannelFuture f = PacketUtil.writeAndFlush(ctx.channel(), CloseMessage.newBuilder().setError(error));
        f.addListener(ChannelFutureListener.CLOSE);
      } else {
        PacketUtil.writeAndFlush(ctx.channel(), error);
      }

    } catch (final Exception ex) {
      logger.log(Level.SEVERE, "ERROR trying to close socket because we got an unhandled exception",
          ex);
      ctx.close();
    }
  }

}
