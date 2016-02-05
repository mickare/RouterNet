package de.rennschnitzel.net.core;

import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.function.Consumer;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.MoreExecutors;

import de.rennschnitzel.net.ProtocolUtils;
import de.rennschnitzel.net.core.Node.HomeNode;
import de.rennschnitzel.net.core.packet.Packer;
import de.rennschnitzel.net.core.procedure.ProcedureCall;
import de.rennschnitzel.net.core.tunnel.TunnelMessage;
import de.rennschnitzel.net.exception.ProtocolException;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureResponseMessage;
import de.rennschnitzel.net.util.concurrent.CloseableLock;
import de.rennschnitzel.net.util.concurrent.CloseableReadWriteLock;
import de.rennschnitzel.net.util.concurrent.ReentrantCloseableReadWriteLock;
import de.rennschnitzel.net.util.function.Callback;

public abstract class AbstractClientNetwork extends AbstractNetwork {

  public AbstractClientNetwork(ScheduledExecutorService executor, HomeNode home) {
    super(executor, home);
  }

  private final CloseableReadWriteLock connectionLock = new ReentrantCloseableReadWriteLock();
  private final Condition connectedCondition = connectionLock.writeLock().newCondition();
  private Connection connection = null;
  private final Map<Callback<AbstractClientNetwork>, ExecutorService> connectCallbacks = Maps.newConcurrentMap();

  /**
   * When the client is connected and ready to send messages this method returns true.
   * 
   * @return true if it is possible to send messages.
   */
  public boolean isConnected() {
    try (CloseableLock l = connectionLock.readLock().open()) {
      return connection != null ? connection.isActive() : false;
    }
  }

  /**
   * Await that the client is connected to the network.
   * 
   * @throws InterruptedException
   */
  public void awaitConnected() throws InterruptedException {
    try (CloseableLock l = connectionLock.writeLock().open()) {
      if (isConnected()) {
        return;
      }
      connectedCondition.await();
    }
  }

  /**
   * Causes the current thread to wait until it is signalled or interrupted, or the specified
   * waiting time elapses.
   * 
   * @param time the maximum time to wait
   * @param unit the time unit of the time argument
   * @return false if the waiting time detectably elapsed before return from the method, else true
   * @throws InterruptedException - if the current thread is interrupted (and interruption of thread
   *         suspension is supported)
   */
  public boolean awaitConnected(long time, TimeUnit unit) throws InterruptedException {
    try (CloseableLock l = connectionLock.writeLock().open()) {
      if (isConnected()) {
        return true;
      }
      return connectedCondition.await(time, unit);
    }
  }

  private static final ExecutorService DIRECT_EXECUTOR = MoreExecutors.newDirectExecutorService();

  private void runConnectListener(final Callback<AbstractClientNetwork> callback, final ExecutorService executor) {
    executor.execute(() -> {
      if (AbstractClientNetwork.this.isConnected()) {
        callback.call(AbstractClientNetwork.this);
      }
    });
  }

  /**
   * Add a callback that is directly executed when the server is connected.
   * 
   * @param callback that is called
   */
  public void addConnectCallback(final Callback<AbstractClientNetwork> callback) {
    this.addConnectCallback(callback, DIRECT_EXECUTOR);
  }

  /**
   * Adds a callback that is executed with the executor when the server is connected.
   * 
   * @param callback that is called with executor
   * @param executor that is used to run the callback
   */
  public void addConnectCallback(final Callback<AbstractClientNetwork> callback, final ExecutorService executor) {
    Preconditions.checkNotNull(callback);
    Preconditions.checkNotNull(executor);
    try (CloseableLock l = connectionLock.readLock().open()) {

      if (this.isConnected()) {
        runConnectListener(callback, executor);

      } else {
        synchronized (connectCallbacks) {

          if (this.isConnected()) {
            runConnectListener(callback, executor);
          } else {
            connectCallbacks.put(callback, executor);
          }

        }
      }

    }
  }

  private void runConnectCallbacks() {
    synchronized (connectCallbacks) {
      for (Entry<Callback<AbstractClientNetwork>, ExecutorService> e : this.connectCallbacks.entrySet()) {
        runConnectListener(e.getKey(), e.getValue());
      }
      this.connectCallbacks.clear();
    }
  }

  @Override
  protected void addConnection(final Connection connection) {
    Preconditions.checkNotNull(connection);
    try (CloseableLock l = connectionLock.writeLock().open()) {
      this.connection = connection;
      String name = connection.getName();
      getLogger().info(connection.getPeerId() + (name != null ? "(" + name + ")" : "") + " connected.");
      connectedCondition.signalAll();
    }
    runConnectCallbacks();
  }

  protected void removeConnection(final Connection connection) {
    Preconditions.checkNotNull(connection);
    try (CloseableLock l = connectionLock.writeLock().open()) {
      if (this.connection == connection) {
        this.connection = null;
      }
      if (connection.isActive()) {
        connection.getChannel().close();
      }
      String name = connection.getName();
      getLogger().info(connection.getPeerId() + (name != null ? "(" + name + ")" : "") + " disconnected.");
    }
  }

  @Override
  public <T, R> void sendProcedureCall(ProcedureCall<T, R> call) {


    if (!call.getTarget().isOnly(this.getHome())) {
      ProcedureMessage.Builder b = ProcedureMessage.newBuilder();
      b.setTarget(call.getTarget().getProtocolMessage());
      b.setSender(ProtocolUtils.convert(getHome().getId()));
      b.setCall(call.toProtocol());
      if (!send(Packer.pack(b.build()))) {
        call.setException(new NotConnectedException());
      }
    }
    if (call.getTarget().contains(this.getHome())) {
      this.getProcedureManager().handle(call);
    }
  }

  @Override
  protected void sendProcedureResponse(final UUID receiverId, final ProcedureResponseMessage msg) throws ProtocolException {
    sendProcedureResponse(this.getHome().getId(), receiverId, msg);
  }

  @Override
  protected void sendProcedureResponse(final UUID senderId, final UUID receiverId, final ProcedureResponseMessage msg)
      throws ProtocolException {
    final ProcedureMessage pmsg = ProcedureMessage.newBuilder().setSender(ProtocolUtils.convert(senderId))
        .setTarget(Target.to(receiverId).getProtocolMessage()).setResponse(msg).build();
    if (this.getHome().getId().equals(receiverId)) {
      this.getProcedureManager().handle(pmsg);
    } else {
      send(Packer.pack(pmsg));
    }
  }


  private boolean send(final Packet packet) {
    try (CloseableLock l = connectionLock.readLock().open()) {
      if (connection != null) {
        connection.writeAndFlushFast(packet);
        return true;
      }
    }
    return false;
  }

  private boolean send(final Consumer<Connection> out) {
    try (CloseableLock l = connectionLock.readLock().open()) {
      if (connection != null) {
        out.accept(connection);
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean publishHomeNodeUpdate() {
    return send(getHome()::sendUpdate);
  }

  @Override
  protected boolean sendTunnelMessage(final TunnelMessage cmsg) {
    return send(Packer.pack(cmsg.toProtocolMessage()));
  }

  @Override
  protected boolean registerTunnel(final Tunnel tunnel) {
    return send(tunnel::register);
  }

}
