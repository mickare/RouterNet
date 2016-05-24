package de.mickare.metricweb.websocket;

import java.util.List;

import de.mickare.metricweb.protocol.WebProtocol;
import de.mickare.metricweb.protocol.WebProtocol.PacketMessage;
import de.rennschnitzel.net.router.Router;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public @RequiredArgsConstructor class WebProtocolDecoder
    extends MessageToMessageDecoder<WebSocketFrame> {

  private @NonNull final WebProtocol protocol;

  @Override
  protected void decode(ChannelHandlerContext ctx, WebSocketFrame frame, List<Object> out)
      throws Exception {

    if (frame instanceof TextWebSocketFrame) {

      PacketMessage msg = protocol.decode(((TextWebSocketFrame) frame).text());
      // Router.getInstance().getLogger().info(msg.toString());
      out.add(msg);

    } else {
      String message = "unsupported frame type: " + frame.getClass().getName();
      throw new UnsupportedOperationException(message);
    }

  }

}
