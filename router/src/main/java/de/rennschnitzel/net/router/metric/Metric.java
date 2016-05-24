package de.rennschnitzel.net.router.metric;

import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.traffic.CustomGlobalChannelTrafficShapingHandler;
import lombok.Getter;

public class Metric {

  private @Getter CustomGlobalChannelTrafficShapingHandler channelTrafficHandler;
  private @Getter PacketCounterHandler<Packet> packetCounterHandler;

  public Metric(EventLoopGroup group) {
    this.channelTrafficHandler = new CustomGlobalChannelTrafficShapingHandler(group, 1000);
    this.packetCounterHandler = new PacketCounterHandler<>(group, Packet.class, 1000);
  }

}
