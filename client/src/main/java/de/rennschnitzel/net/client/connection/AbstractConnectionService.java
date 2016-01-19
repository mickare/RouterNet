package de.rennschnitzel.net.client.connection;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.ListenableFuture;

import de.rennschnitzel.net.Net;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.exception.NotConnectedException;
import de.rennschnitzel.net.netty.ConnectionFuture;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ErrorMessage;
import de.rennschnitzel.net.util.concurrent.CloseableLock;
import de.rennschnitzel.net.util.concurrent.CloseableReadWriteLock;
import de.rennschnitzel.net.util.concurrent.ReentrantCloseableReadWriteLock;

public abstract class AbstractConnectionService<F extends ConnectionFuture<?>>
    extends AbstractScheduledService implements ConnectionService {

  private static final CloseMessage CLOSEMSG_SHUTDOWN =
      CloseMessage.newBuilder().setShutdown(true).build();
  private static final CloseMessage CLOSEMSG_NORMAL_INACTIVE =
      CloseMessage.newBuilder().setNormal("inactive").build();

  private final CloseableReadWriteLock lock = new ReentrantCloseableReadWriteLock(true);

  private F connection = null;

  private final ScheduledExecutorService executor;

  public AbstractConnectionService(ScheduledExecutorService executor) {
    Preconditions.checkArgument(!executor.isShutdown());
    this.executor = executor;
  }

  @Override
  protected ScheduledExecutorService executor() {
    return executor;
  }

  @Override
  public Connection getConnection(long timeout, TimeUnit unit) throws NotConnectedException {
    Preconditions.checkArgument(timeout >= 0);
    Preconditions.checkNotNull(unit);
    switch (this.state()) {
      case RUNNING:
      case STARTING:
        break;
      default:
        if (this.state() == State.FAILED) {
          throw new NotConnectedException(this.failureCause());
        }
        throw new NotConnectedException(
            "Connection service is not running (state: " + this.state() + ")!");
    }
    try (CloseableLock l = lock.readLock().open(timeout, unit)) {
      return connection.get(timeout, unit);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new NotConnectedException(e);
    }
  }

  @Override
  public ListenableFuture<? extends Connection> getConnectionFuture() {
    return connection;
  }

  protected abstract F connect();

  protected abstract void disconnect(F con, CloseMessage msg);


  private void disconnect(F con, CloseMessage.Builder msg) {
    this.disconnect(con, msg.build());
  }

  private void disconnectError(F con, Throwable t) {
    this.disconnect(con, CloseMessage.newBuilder().setError(ErrorMessage.newBuilder()
        .setType(ErrorMessage.Type.SERVER_ERROR).setMessage(t.getMessage())));
  }


  private void disconnectShutDown(F con) {
    this.disconnect(con, CLOSEMSG_SHUTDOWN);
  }

  private void disconnectNormal(F con, String reason) {
    this.disconnect(con, CloseMessage.newBuilder().setNormal(reason));
  }


  private void severe(Throwable t, String prefixMsg) {
    Net.getLogger().log(Level.SEVERE, prefixMsg + ":\n" + t.getMessage(), t);
  }

  @Override
  protected void runOneIteration() throws InterruptedException {
    boolean reconnect = false;

    try (CloseableLock l = lock.writeLock().open()) {

      if (this.connection != null) {
        if (!this.connection.isOpen()) {
          disconnect(this.connection, CLOSEMSG_NORMAL_INACTIVE);
          reconnect = true;

        } else {

          try {
            Connection con = connection.get(3, TimeUnit.SECONDS);
            if (con.isClosed()) {
              reconnect = true;
            }
          } catch (ExecutionException | TimeoutException e) {
            severe(e, "Failed to get connection");
            disconnectError(connection, e);
            reconnect = true;
          }

        }
      }

      if (reconnect) {
        reconnect();
      }
    }

  }

  public void reconnect() {
    try (CloseableLock l = lock.writeLock().open()) {
      if (!isRunning()) {
        return;
      }
      if (this.connection != null && this.connection.isOpen()) {
        this.disconnectNormal(connection, "reconnect");
      }
      this.connection = connect();
    }
  }

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedDelaySchedule(1, 20, TimeUnit.MILLISECONDS);
  }

  @Override
  protected void startUp() {
    try {
      this.connection = connect();
    } catch (Exception e) {
      Net.getLogger().log(Level.WARNING, "Exception while connecting:\n" + e.getMessage(), e);
    }
  }

  @Override
  protected void shutDown() {
    try {
      this.disconnectShutDown(connection);
    } catch (Exception e) {
      severe(e, "Failed to disconnect");
    }
  }

}
