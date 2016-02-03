package de.rennschnitzel.net.core;

import java.util.UUID;
import java.util.function.Consumer;

import com.google.common.base.Preconditions;

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

public abstract class AbstractClientNetwork extends AbstractNetwork {

  public AbstractClientNetwork(HomeNode home) {
    super(home);
  }

  private final CloseableReadWriteLock connectionLock = new ReentrantCloseableReadWriteLock();
  private Connection connection = null;

  public boolean isConnected() {
    try (CloseableLock l = connectionLock.readLock().open()) {
      return connection != null;
    }
  }

  @Override
  protected void addConnection(final Connection connection) {
    Preconditions.checkNotNull(connection);
    try (CloseableLock l = connectionLock.writeLock().open()) {
      this.connection = connection;
      getLogger().info(connection.getPeerId() + " connected.");
    }
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
      getLogger().info(connection.getPeerId() + " disconnected.");
    }
  }

  @Override
  public <T, R> boolean sendProcedureCall(ProcedureCall<T, R> call) {

    boolean success = true;

    if (!call.getTarget().isOnly(this.getHome())) {
      ProcedureMessage.Builder b = ProcedureMessage.newBuilder();
      b.setTarget(call.getTarget().getProtocolMessage());
      b.setSender(ProtocolUtils.convert(getHome().getId()));
      b.setCall(call.toProtocol());
      success &= send(Packer.pack(b.build()));
    }
    if (call.getTarget().contains(this.getHome())) {
      this.getProcedureManager().handle(call);
    }
    return success;
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
