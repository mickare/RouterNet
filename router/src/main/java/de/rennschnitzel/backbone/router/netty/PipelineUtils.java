package de.rennschnitzel.backbone.router.netty;

import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;

import de.rennschnitzel.backbone.router.Router;
import de.rennschnitzel.backbone.router.util.Utils;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.internal.PlatformDependent;

public class PipelineUtils {


  private static boolean epoll;

  static {
    if (!PlatformDependent.isWindows() && Boolean.parseBoolean(System.getProperty("bungee.epoll", "false"))) {
      Router.getInstance().getLogger().info("Not on Windows, attempting to use enhanced EpollEventLoop");
      if (epoll = Epoll.isAvailable()) {
        Router.getInstance().getLogger().info("Epoll is working, utilising it!");
      } else {
        Router.getInstance().getLogger().log(Level.WARNING, "Epoll is not working, falling back to NIO: {0}",
            Utils.exception(Epoll.unavailabilityCause()));
      }
    }
  }

  public static EventLoopGroup newEventLoopGroup(int threads, ThreadFactory factory) {
    return epoll ? new EpollEventLoopGroup(threads, factory) : new NioEventLoopGroup(threads, factory);
  }

  public PipelineUtils() {
    // TODO Auto-generated constructor stub
  }

}
