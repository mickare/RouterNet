package de.mickare.metricweb.websocket;

import java.util.List;

import de.mickare.metricweb.protocol.WebProtocol;
import de.mickare.metricweb.protocol.WebProtocol.PacketMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public @RequiredArgsConstructor class WebProtocolEncoder
    extends MessageToMessageEncoder<PacketMessage> {

  private @NonNull final WebProtocol protocol;

  @Override
  protected void encode(ChannelHandlerContext ctx, PacketMessage msg, List<Object> out)
      throws Exception {
    out.add(protocol.encode(msg));
  }

}
