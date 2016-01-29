package de.rennschnitzel.net.client.connection;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractScheduledService;

import de.rennschnitzel.net.NetClient;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.login.ClientLoginEngine;
import de.rennschnitzel.net.core.login.LoginEngine;
import de.rennschnitzel.net.core.packet.BasePacketHandler;
import de.rennschnitzel.net.core.packet.PacketHandler;
import de.rennschnitzel.net.util.FutureUtils;
import de.rennschnitzel.net.util.concurrent.CloseableLock;
import de.rennschnitzel.net.util.concurrent.CloseableReadWriteLock;
import de.rennschnitzel.net.util.concurrent.ReentrantCloseableReadWriteLock;
import io.netty.util.concurrent.Future;
import lombok.Getter;

public abstract class AbstractConnectService<L extends ClientLoginEngine<?>, C extends Connection>
    extends AbstractScheduledService implements ConnectService<L> {

  @Getter
  private final NetClient client;

  @Getter
  private final long delay_time;
  @Getter
  private final TimeUnit delay_unit;

  private final CloseableReadWriteLock lock = new ReentrantCloseableReadWriteLock();
  private Future<?> connectFuture = null;
  private volatile L loginHandler = null;

  public AbstractConnectService(NetClient client) {
    this(client, 10, TimeUnit.SECONDS);
  }

  public AbstractConnectService(NetClient client, long delay_time, TimeUnit delay_unit) {
    Preconditions.checkNotNull(client);
    Preconditions.checkArgument(delay_time > 0);
    Preconditions.checkArgument(delay_unit.toSeconds(delay_time) > 0);
    this.client = client;
    this.delay_time = delay_time;
    this.delay_unit = delay_unit;
  }

  public Logger getLogger() {
    return client.getLogger();
  }

  protected void startUp() throws Exception {
    connectSoft().await(100);
    getLogger().info("Connect service started");
  }

  protected void shutDown() throws Exception {
    disconnect();
  }

  private void disconnect() {
    try (CloseableLock l = lock.writeLock().open()) {
      if (loginHandler != null) {
        loginHandler.tryDisconnect("no reason");
      }
      this.loginHandler = null;
    }
  }

  @Override
  protected void runOneIteration() throws Exception {
    connectSoft().await(100);
  }

  private Future<?> connectSoft() {
    try (CloseableLock l = lock.writeLock().open()) {
      if (loginHandler != null) {
        if (loginHandler.isContextActive()) {
          return FutureUtils.SUCCESS;
        }
        disconnect();
      }
      return connect();
    }
  }

  private Future<?> connect() {
    try (CloseableLock l = lock.writeLock().open()) {
      Preconditions.checkState(this.loginHandler == null);
      this.loginHandler = createLoginHandler();
      Preconditions.checkState(this.loginHandler.getState() == LoginEngine.State.NEW);

      FutureUtils.on(this.loginHandler.getConnectionPromise(), f -> {
        if (f.isSuccess()) {
          getLogger().info("Connected with " + f.get().getId());
        }
      });



      connectFuture = doConnect(this.loginHandler);
      return connectFuture;
    }
  }

  protected abstract Future<?> doConnect(L loginHandler);

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
