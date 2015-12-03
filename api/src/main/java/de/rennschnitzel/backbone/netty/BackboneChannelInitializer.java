package de.rennschnitzel.backbone.netty;

import java.util.logging.Logger;

import de.rennschnitzel.backbone.net.protocol.TransportProtocol;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BackboneChannelInitializer extends ChannelInitializer<SocketChannel> {

  @NonNull
  @Getter
  private final Logger logger;

  @NonNull
  private final ChannelInboundHandler handshake;
  
  @Override
  public void initChannel(SocketChannel ch) throws Exception {
    
 // Attributes & config
    // ch.attr(PipeUtils.LOGGER).set(getLogger());
    ch.config().setAllocator(PooledByteBufAllocator.DEFAULT);

    // Pipeline
    ChannelPipeline pipeline = ch.pipeline();
    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
    pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
    pipeline.addLast("protoDecoder",
        new ProtobufDecoder(TransportProtocol.Packet.getDefaultInstance()));
    pipeline.addLast("protoEncoder", new ProtobufEncoder());
    pipeline.addLast("handshake", handshake);
    pipeline.addLast("exception", new ExceptionHandler(getLogger()));
    
    
  }  

}
