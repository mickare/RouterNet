package de.rennschnitzel.net.client.connection;

import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.ListenableFuture;

import de.rennschnitzel.net.NetClient;
import de.rennschnitzel.net.NetConstants;
import de.rennschnitzel.net.Network;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.packet.BasePacketHandler;
import de.rennschnitzel.net.core.packet.PacketHandler;
import de.rennschnitzel.net.netty.BaseChannelInitializer;
import de.rennschnitzel.net.netty.MainHandler;
import de.rennschnitzel.net.netty.NettyClient;
import de.rennschnitzel.net.netty.NettyConnection;
import de.rennschnitzel.net.netty.login.NettyLoginClientHandler;
import de.rennschnitzel.net.util.concurrent.CloseableLock;
import de.rennschnitzel.net.util.concurrent.CloseableReadWriteLock;
import de.rennschnitzel.net.util.concurrent.ReentrantCloseableReadWriteLock;
import io.netty.channel.ChannelHandlerContext;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NettyConnectService extends AbstractScheduledService
    implements ConnectService<ChannelHandlerContext, NettyConnection<Network>> {


  @NonNull
  private final NetClient client;

  private final CloseableReadWriteLock lock = new ReentrantCloseableReadWriteLock();
  private volatile NettyLoginClientHandler loginHandler = null;
  private NettyClient connector = null;

  @Override
  public ListenableFuture<Connection> getConnectionPromise() {
    return getLoginHandler().getConnectionFuture();
  }

  @Override
  public String getName() {
    return getLoginHandler().getName();
  }

  @Override
  public UUID getId() {
    return getLoginHandler().getId();
  }

  @Override
  protected void runOneIteration() throws Exception {
    reconnectSoft();
  }

  public boolean waitConnected() {
    try (CloseableLock l = lock.readLock().open()) {
      if (loginHandler == null) {
        return false;
      }
      try {
        ListenableFuture<Connection> conFuture = loginHandler.getConnectionFuture();
        if (loginHandler.isSuccess() && conFuture.isDone()) {
          Connection connection = conFuture.get(500, TimeUnit.MILLISECONDS);
          if (connection.isActive()) {
            return true;
          } else {
            return false;
          }
        }

        Connection connection = conFuture.get(5, TimeUnit.MILLISECONDS);
        if (connection.isActive()) {
          return true;
        }

      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        loginHandler.fail(e);
        this.client.getLogger().log(Level.WARNING, "failed connection: " + e.getMessage(), e);
      }
    }
    return false;
  }

  public void reconnectSoft() {
    synchronized (this) {
      if (waitConnected()) {
        // connected;
        return;
      }
      if (this.connector != null) {
        this.connector.disconnect();
      }

      try (CloseableLock l = lock.writeLock().open()) {

        if (this.loginHandler != null && !this.loginHandler.isDone()) {
          this.loginHandler.fail(new TimeoutException("login took too much time"));
        }

        this.loginHandler =
            new NettyLoginClientHandler(client.getNetwork(), client.getAuthentication());

        PacketHandler<NettyConnection<Network>> client_packetHandler =
            new BasePacketHandler<NettyConnection<Network>>();

        BaseChannelInitializer clientInit =
            new BaseChannelInitializer(() -> new MainHandler<Network>(client.getNetwork(), //
                this.loginHandler, client_packetHandler));

        this.connector = new NettyClient("NettyClient",
            HostAndPort.fromString(client.getConfig().getConnection().getAddress())
                .withDefaultPort(NetConstants.DEFAULT_PORT),
            clientInit);
      }

    }
    this.connector.connect();
  }

  public void reconnectHard() {

    synchronized (this) {
      try (CloseableLock l = lock.readLock().open()) {
        if (this.loginHandler != null) {
          this.loginHandler.fail(new CancellationException());
          if (this.loginHandler.isSuccess()) {
            Connection con =
                this.loginHandler.getConnectionFuture().get(100, TimeUnit.MILLISECONDS);
            con.disconnect("hard reconnect");
          }
        }
      } catch (InterruptedException | TimeoutException | ExecutionException e) {
      }
    }

    reconnectSoft();

  }

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedDelaySchedule(0, 10, TimeUnit.SECONDS);
  }

  @Override
  public NettyLoginClientHandler getLoginHandler() {
    try (CloseableLock l = lock.readLock().open()) {
      return loginHandler;
    }
  }

}
