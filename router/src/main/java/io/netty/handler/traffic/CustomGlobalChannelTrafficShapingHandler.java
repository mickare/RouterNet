package io.netty.handler.traffic;

import java.util.concurrent.ScheduledExecutorService;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

public class CustomGlobalChannelTrafficShapingHandler extends GlobalChannelTrafficShapingHandler {

  public CustomGlobalChannelTrafficShapingHandler(ScheduledExecutorService executor) {
    super(executor);
  }

  public CustomGlobalChannelTrafficShapingHandler(ScheduledExecutorService executor,
      long checkInterval) {
    super(executor, checkInterval);
  }

  public CustomGlobalChannelTrafficShapingHandler(ScheduledExecutorService executor,
      long writeGlobalLimit, long readGlobalLimit, long writeChannelLimit, long readChannelLimit) {
    super(executor, writeGlobalLimit, readGlobalLimit, writeChannelLimit, readChannelLimit);
  }

  public CustomGlobalChannelTrafficShapingHandler(ScheduledExecutorService executor,
      long writeGlobalLimit, long readGlobalLimit, long writeChannelLimit, long readChannelLimit,
      long checkInterval) {
    super(executor, writeGlobalLimit, readGlobalLimit, writeChannelLimit, readChannelLimit,
        checkInterval);
  }

  public CustomGlobalChannelTrafficShapingHandler(ScheduledExecutorService executor,
      long writeGlobalLimit, long readGlobalLimit, long writeChannelLimit, long readChannelLimit,
      long checkInterval, long maxTime) {
    super(executor, writeGlobalLimit, readGlobalLimit, writeChannelLimit, readChannelLimit,
        checkInterval, maxTime);
  }

  public TrafficCounter getTrafficCounter(ChannelHandlerContext ctx) {
    return getTrafficCounter(ctx.channel());
  }

  public TrafficCounter getTrafficCounter(Channel channel) {
    PerChannel perChannel = this.channelQueues.get(channel.hashCode());
    if (perChannel != null) {
      return perChannel.channelTrafficCounter;
    }
    return null;
  }

}
