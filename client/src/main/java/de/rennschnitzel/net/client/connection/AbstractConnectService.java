package de.rennschnitzel.net.client.connection;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.rennschnitzel.net.NetClient;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.NotConnectedException;
import de.rennschnitzel.net.netty.PipelineUtils;
import de.rennschnitzel.net.service.ConnectClient;
import de.rennschnitzel.net.util.FutureUtils;
import de.rennschnitzel.net.util.concurrent.CloseableLock;
import de.rennschnitzel.net.util.concurrent.CloseableReadWriteLock;
import de.rennschnitzel.net.util.concurrent.ReentrantCloseableReadWriteLock;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Promise;
import lombok.AccessLevel;
import lombok.Getter;

public abstract class AbstractConnectService extends AbstractScheduledService
    implements ConnectService {

  private @Getter final NetClient client;
  private @Getter final long delay_time;
  private @Getter final TimeUnit delay_unit;

  private final CloseableReadWriteLock lock = new ReentrantCloseableReadWriteLock();
  private @Getter Promise<Connection> currentFuture = null;
  private ConnectClient connect = null;
  private @Getter(AccessLevel.PROTECTED) EventLoopGroup group;

  private final Set<Consumer<Connection>> listeners = Sets.newIdentityHashSet();

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

  public boolean addListener(Consumer<Connection> listener) {
    return listeners.add(listener);
  }

  @Override
  public Logger getLogger() {
    return client.getLogger();
  }

  protected void startUp() throws Exception {
    group = PipelineUtils.newEventLoopGroup(0,
        new ThreadFactoryBuilder().setNameFormat("Net-Netty IO Thread #%1$d").build());
    getLogger().info("Connect service started");
    
    runOneIteration();    
  }

  protected void shutDown() throws Exception {
    disconnect();
    group.shutdownGracefully().awaitUninterruptibly();
  }

  @Override
  protected void runOneIteration() throws Exception {
    ConnectClient con = connectSoft();
    if (con != null) {
      if (this.isRunning()) {
        con.awaitRunning(1000);
      } else {
        con.close();
      }
    }
  }

  private void disconnect() {
    try (CloseableLock l = lock.writeLock().open()) {
      if (connect != null) {
        connect.close();
      }
    }
  }

  private ConnectClient connectSoft() {
    if (connect == null || connect.isClosed() && this.isRunning()) {
      if (currentFuture != null && !currentFuture.isDone()) {
        currentFuture.tryFailure(new NotConnectedException("timed out"));
      }
      try (CloseableLock l = lock.writeLock().open()) {
        currentFuture = FutureUtils.newPromise();
        connect = newConnectClient(currentFuture).connect();
      }
    }
    return connect;
  }

  protected abstract ConnectClient newConnectClient(Promise<Connection> future);

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedDelaySchedule(delay_time, delay_time, delay_unit);
  }

}
