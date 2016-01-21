package de.rennschnitzel.net.netty;

import java.util.function.Supplier;

import de.rennschnitzel.net.protocol.TransportProtocol;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.compression.FastLzFrameDecoder;
import io.netty.handler.codec.compression.FastLzFrameEncoder;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BaseChannelInitializer extends ChannelInitializer<SocketChannel> {

  @NonNull
  private final Supplier<MainHandler<?>> mainHandler;

  @Override
  public void initChannel(SocketChannel ch) throws Exception {

    // Attributes & config
    // ch.attr(PipeUtils.LOGGER).set(getLogger());
    ch.config().setAllocator(PooledByteBufAllocator.DEFAULT);

    // Pipeline
    ChannelPipeline pipeline = ch.pipeline();
    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
    pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
    pipeline.addLast("compressionDecoder", new FastLzFrameDecoder(true));
    pipeline.addLast("compressionEncoder", new FastLzFrameEncoder(true));
    pipeline.addLast("protoDecoder",
        new ProtobufDecoder(TransportProtocol.Packet.getDefaultInstance()));
    pipeline.addLast("protoEncoder", new ProtobufEncoder());
    pipeline.addLast("main", mainHandler.get());

  }

}
