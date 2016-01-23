package de.rennschnitzel.net.core;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import de.rennschnitzel.net.ProtocolUtils;
import de.rennschnitzel.net.core.Node.HomeNode;
import de.rennschnitzel.net.core.procedure.ProcedureCall;
import de.rennschnitzel.net.core.tunnel.TunnelMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureResponseMessage;
import de.rennschnitzel.net.util.FutureUtils;
import de.rennschnitzel.net.util.concurrent.CloseableLock;
import de.rennschnitzel.net.util.concurrent.CloseableReadWriteLock;
import de.rennschnitzel.net.util.concurrent.ReentrantCloseableReadWriteLock;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;

public abstract class AbstractClientNetwork extends AbstractNetwork implements PacketOutWriter {

  public AbstractClientNetwork(HomeNode home) {
    super(home);
  }

  private final CloseableReadWriteLock connectionLock = new ReentrantCloseableReadWriteLock();
  private Promise<Connection> connectionPromise = ImmediateEventExecutor.INSTANCE.newPromise();

  public Future<Connection> getConnectionFuture() {
    try (CloseableLock l = connectionLock.readLock().open()) {
      return this.connectionPromise;
    }
  }

  @Override
  protected synchronized boolean addConnection0(final Connection connection) throws Exception {
    try (CloseableLock l = connectionLock.writeLock().open()) {
      final Promise<Connection> old = this.connectionPromise;
      if (!old.isDone()) {
        old.setSuccess(connection);
        return true;
      } else if (old.isSuccess()) {
        Connection con = old.get();
        if (con == connection) {
          return false;
        }
      }
      this.connectionPromise = ImmediateEventExecutor.INSTANCE.<Connection>newPromise().setSuccess(connection);
      return false;
    }
  }

  @Override
  protected boolean removeConnection0(final Connection connection) {
    try (CloseableLock l = connectionLock.writeLock().open()) {
      final Promise<Connection> old = this.connectionPromise;
      if (old.isSuccess()) {
        Connection con;
        try {
          con = old.get();
          if (con == connection) {
            this.connectionPromise = ImmediateEventExecutor.INSTANCE.newPromise();
            return true;
          }
        } catch (InterruptedException | ExecutionException e) {
          // will not happen
        }
      }
      return false;
    }
  }

  @Override
  public <T, R> Future<?> sendProcedureCall(ProcedureCall<T, R> call) {
    try {

      ProcedureMessage.Builder b = ProcedureMessage.newBuilder();
      b.setTarget(call.getTarget().getProtocolMessage());
      b.setSender(ProtocolUtils.convert(getHome().getId()));
      b.setCall(call.toProtocol());
      ProcedureMessage msg = b.build();
      Packet packet = Packet.newBuilder().setProcedureMessage(msg).build();

      Future<?> future = FutureUtils.SUCCESS;
      if (!call.getTarget().isOnly(this.getHome())) {
        future = send(packet);

      }
      if (call.getTarget().contains(this.getHome())) {
        this.getProcedureManager().handle(call);
      }
      return future;

    } catch (Exception e) {
      return FutureUtils.futureFailure(e);
    }
  }

  public Future<?> send(Packet packet) {
    return FutureUtils.combine(getConnectionFuture(), con -> con.send(packet));
  }

  public Future<?> send(Packet.Builder packet) {
    return send(packet.build());
  }

  @Override
  public Future<?> sendProcedureResponse(UUID receiverId, ProcedureResponseMessage build) {
    ProcedureMessage pmsg = ProcedureMessage.newBuilder().setSender(ProtocolUtils.convert(getHome().getId()))
        .setTarget(Target.to(receiverId).getProtocolMessage()).setResponse(build).build();
    if (this.getHome().getId().equals(receiverId)) {
      try {
        this.getProcedureManager().handle(this, pmsg);
        return FutureUtils.SUCCESS;
      } catch (Exception e) {
        return FutureUtils.futureFailure(e);
      }
    } else {
      return send(Packet.newBuilder().setProcedureMessage(pmsg));
    }
  }


  @Override
  public void publishHomeNodeUpdate() {
    this.getHome().sendUpdate(this);
  }

  @Override
  protected Future<?> sendTunnelMessage(TunnelMessage cmsg) {
    return FutureUtils.combine(this.getConnectionFuture(), con -> {
      return send(Packet.newBuilder().setTunnelMessage(cmsg.toProtocolMessage(con)));
    });
  }

  @Override
  protected Future<?> registerTunnel(Tunnel tunnel) {
    return FutureUtils.combine(this.getConnectionFuture(), con -> {
      return FutureUtils.transformFuture(con.registerTunnel(tunnel));
    });
  }

}
