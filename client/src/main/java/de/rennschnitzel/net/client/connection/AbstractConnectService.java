package de.rennschnitzel.net.client.connection;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractScheduledService;

import de.rennschnitzel.net.NetClient;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.login.LoginClientHandler;
import de.rennschnitzel.net.core.login.LoginHandler;
import de.rennschnitzel.net.core.packet.BasePacketHandler;
import de.rennschnitzel.net.core.packet.PacketHandler;
import de.rennschnitzel.net.exception.ConnectionException;
import de.rennschnitzel.net.protocol.TransportProtocol.ErrorMessage;
import de.rennschnitzel.net.util.FutureUtils;
import de.rennschnitzel.net.util.concurrent.CloseableLock;
import de.rennschnitzel.net.util.concurrent.CloseableReadWriteLock;
import de.rennschnitzel.net.util.concurrent.ReentrantCloseableReadWriteLock;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractConnectService<L extends LoginClientHandler<?>, C extends Connection>
    extends AbstractScheduledService implements ConnectService<L> {

  @NonNull
  private final NetClient client;

  private final long delay_time = 10;
  private final TimeUnit delay_unit = TimeUnit.SECONDS;


  private final CloseableReadWriteLock lock = new ReentrantCloseableReadWriteLock();
  private Future<?> connectFuture = null;
  private volatile L loginHandler = null;

  protected void startUp() throws Exception {
    connect().await(100);
  }

  protected void shutDown() throws Exception {
    disconnect();
  }

  private void disconnect() {
    try (CloseableLock l = lock.writeLock().open()) {
      if (loginHandler != null) {
        loginHandler.fail(new ConnectionException(ErrorMessage.Type.UNAVAILABLE, "shutdown"));
        if (loginHandler.isSuccess()) {
          FutureUtils.on(loginHandler.getConnectionPromise(), fcon -> {
            if (fcon.isSuccess()) {
              fcon.get().disconnect("shutdown");
            }
          });
        }
      }
      this.loginHandler = null;
    }
  }

  @Override
  protected void runOneIteration() throws Exception {
    connectSoft();
  }

  private void connectSoft() {
    try (CloseableLock l = lock.writeLock().open()) {
      if (loginHandler != null) {
        if (loginHandler.isContextActive()) {
          return;
        }
        loginHandler = null;
      }
      connect();
    }
  }

  private Future<?> connect() {
    try (CloseableLock l = lock.writeLock().open()) {
      Preconditions.checkState(this.loginHandler == null);
      this.loginHandler = createLoginHandler();
      Preconditions.checkState(this.loginHandler.getState() == LoginHandler.State.NEW);

      connectFuture = doConnect(this.loginHandler, this.createPacketHandler());
      return connectFuture;
      /*
       * BaseChannelInitializer init = new BaseChannelInitializer(() -> new
       * MainHandler<Network>(client.getNetwork(), this.loginHandler, createPacketHandler()));
       */
    }
  }

  protected abstract Future<?> doConnect(L loginHandler, PacketHandler<C> packetHandler);

  protected abstract L createLoginHandler();

  protected PacketHandler<C> createPacketHandler() {
    return new BasePacketHandler<C>();
  }

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedDelaySchedule(delay_time, delay_time, delay_unit);
  }

  @Override
  public Future<Connection> getConnectionPromise() {
    return getLoginHandler().getConnectionPromise();
  }

  @Override
  public L getLoginHandler() {
    try (CloseableLock l = lock.readLock().open()) {
      return loginHandler;
    }
  }

  @Override
  public String getConnectedName() {
    return getLoginHandler().getName();
  }

  @Override
  public UUID getConnectedId() {
    return getLoginHandler().getId();
  }

}
