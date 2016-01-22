package de.rennschnitzel.net.netty;

import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.util.ThrowableUtils;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.internal.PlatformDependent;

public class PipelineUtils {


  private static boolean epoll;

  static {
    if (!PlatformDependent.isWindows()
        && Boolean.parseBoolean(System.getProperty("bungee.epoll", "false"))) {
      AbstractNetwork.getInstance().getLogger()
          .info("Not on Windows, attempting to use enhanced EpollEventLoop");
      if (epoll = Epoll.isAvailable()) {
        AbstractNetwork.getInstance().getLogger().info("Epoll is working, utilising it!");
      } else {
        AbstractNetwork.getInstance().getLogger().log(Level.WARNING,
            "Epoll is not working, falling back to NIO: {0}",
            ThrowableUtils.exception(Epoll.unavailabilityCause()));
      }
    }
  }

  public static Class<? extends ServerChannel> getServerChannelClass() {
    return epoll ? EpollServerSocketChannel.class : NioServerSocketChannel.class;
  }

  public static Class<? extends Channel> getChannelClass() {
    return epoll ? EpollSocketChannel.class : NioSocketChannel.class;
  }

  public static EventLoopGroup newEventLoopGroup(int threads, ThreadFactory factory) {
    return epoll ? new EpollEventLoopGroup(threads, factory)
        : new NioEventLoopGroup(threads, factory);
  }

  public PipelineUtils() {
    // TODO Auto-generated constructor stub
  }

}
