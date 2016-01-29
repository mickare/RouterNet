package de.rennschnitzel.net.core;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import de.rennschnitzel.net.ProtocolUtils;
import de.rennschnitzel.net.core.Node.HomeNode;
import de.rennschnitzel.net.core.packet.Packer;
import de.rennschnitzel.net.core.procedure.ProcedureCall;
import de.rennschnitzel.net.core.tunnel.TunnelMessage;
import de.rennschnitzel.net.exception.ProtocolException;
import de.rennschnitzel.net.protocol.TransportProtocol;
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

public abstract class AbstractClientNetwork extends AbstractNetwork {

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
  protected boolean addConnection0(final Connection connection) throws Exception {
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
  public <T, R> void sendProcedureCall(ProcedureCall<T, R> call) {

    if (!call.getTarget().isOnly(this.getHome())) {
      ProcedureMessage.Builder b = ProcedureMessage.newBuilder();
      b.setTarget(call.getTarget().getProtocolMessage());
      b.setSender(ProtocolUtils.convert(getHome().getId()));
      b.setCall(call.toProtocol());
      send(Packer.pack(b.build()));

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


  private void send(final Packet packet) {
    FutureUtils.onSuccess(this.getConnectionFuture(), con -> {
      con.writeAndFlushFast(packet);
    });
  }

  @Override
  public void publishHomeNodeUpdate() {
    FutureUtils.onSuccess(this.getConnectionFuture(), getHome()::sendUpdate);
  }

  @Override
  protected void sendTunnelMessage(final TunnelMessage cmsg) {
    final TransportProtocol.TunnelMessage msg = cmsg.toProtocolMessage();
    FutureUtils.onSuccess(this.getConnectionFuture(), con -> {
      con.writeAndFlushFast(msg);
    });
  }

  @Override
  protected void registerTunnel(final Tunnel tunnel) {
    FutureUtils.onSuccess(this.getConnectionFuture(), tunnel::register);
  }

}
