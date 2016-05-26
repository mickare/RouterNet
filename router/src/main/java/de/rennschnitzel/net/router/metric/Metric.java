package de.rennschnitzel.net.router.metric;

import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.traffic.CustomGlobalChannelTrafficShapingHandler;
import lombok.Getter;

public class Metric {

  private @Getter CustomGlobalChannelTrafficShapingHandler channelTrafficHandler;
 // private @Getter ChannelTrafficHandler channelTrafficHandler;
  private @Getter PacketTrafficHandler<Packet> packetTrafficHandler;

  
  public Metric(EventLoopGroup group) {
    this.channelTrafficHandler = new CustomGlobalChannelTrafficShapingHandler(group, 1000);
    this.packetTrafficHandler = new PacketTrafficHandler<>(Packet.class, group, 1000);
  }

}
