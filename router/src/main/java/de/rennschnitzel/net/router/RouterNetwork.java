package de.rennschnitzel.net.router;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.ProtocolUtils;
import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.Node;
import de.rennschnitzel.net.core.Node.HomeNode;
import de.rennschnitzel.net.core.NotConnectedException;
import de.rennschnitzel.net.core.Target;
import de.rennschnitzel.net.core.Tunnel;
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

public class RouterNetwork extends AbstractNetwork {

  private final Router router;

  private final CloseableReadWriteLock connectionLock = new ReentrantCloseableReadWriteLock(true);
  private final Map<UUID, Connection> connections = new ConcurrentHashMap<>();

  protected RouterNetwork(Router router, HomeNode home) {
    super(home);
    Preconditions.checkNotNull(router);
    this.router = router;
  }

  @Override
  public Logger getLogger() {
    return router.getLogger();
  }

  @Override
  public ScheduledExecutorService getExecutor() {
    return router.getScheduler();
  }

  // ********************************************************************
  // CONNECTION

  @Override
  protected void addConnection(Connection connection) {
    try (CloseableLock l = connectionLock.writeLock().open()) {
      Connection old = connections.put(connection.getPeerId(), connection);
      if (old != null && old != connection) {
        old.disconnect("replaced");
      }
      getLogger().info(connection.getPeerId() + " connected.");
    }
  }

  @Override
  protected void removeConnection(Connection connection) {
    try (CloseableLock l = connectionLock.writeLock().open()) {
      if (this.connections.remove(connection.getPeerId(), connection)) {
        getLogger().info(connection.getPeerId() + " disconnected.");
      }
      if (connection.isActive()) {
        connection.getChannel().close();
      }
    }
  }

  protected Connection getConnection(UUID peerId) {
    try (CloseableLock l = connectionLock.readLock().open()) {
      return this.connections.get(peerId);
    }
  }

  // ********************************************************************
  // PROCEDURE

  @Override
  protected <T, R> void sendProcedureCall(ProcedureCall<T, R> call) {

    if (!call.getTarget().isOnly(this.getHome())) {
      ProcedureMessage.Builder b = ProcedureMessage.newBuilder();
      b.setTarget(call.getTarget().getProtocolMessage());
      b.setSender(ProtocolUtils.convert(getHome().getId()));
      b.setCall(call.toProtocol());
      final Packet packet = Packer.pack(b.build());

      for (final UUID id : call.getNodeUUIDs()) {
        if (this.getHome().getId().equals(id)) {
          continue;
        }
        Connection con = this.getConnection(id);
        if (con != null && con.isActive()) {
          con.writeAndFlush(packet).addListener(f -> {
            if (!f.isSuccess()) {
              call.setException(id, f.cause());
            }
          });
          continue;
        }
        call.setException(id, new NotConnectedException());
      }

    }
    if (call.getTarget().contains(this.getHome())) {
      this.getProcedureManager().handle(call);
    }

  }

  @Override
  protected void sendProcedureResponse(UUID receiverId, ProcedureResponseMessage msg)
      throws ProtocolException {
    sendProcedureResponse(this.getHome().getId(), receiverId, msg);
  }

  @Override
  protected void sendProcedureResponse(UUID senderId, UUID receiverId, ProcedureResponseMessage msg)
      throws ProtocolException {
    final ProcedureMessage pmsg =
        ProcedureMessage.newBuilder().setSender(ProtocolUtils.convert(senderId))
            .setTarget(Target.to(receiverId).getProtocolMessage()).setResponse(msg).build();
    if (this.getHome().getId().equals(receiverId)) {
      this.getProcedureManager().handle(pmsg);
    } else {
      Connection con = this.getConnection(receiverId);
      if (con != null) {
        con.writeAndFlushFast(Packer.pack(pmsg));
      }
    }
  }

  // ********************************************************************
  // HOME

  @Override
  protected boolean publishHomeNodeUpdate() {
    try (CloseableLock l = connectionLock.readLock().open()) {
      this.getHome().sendUpdate(this.connections.values());
    }
    return true;
  }


  // ********************************************************************
  // TUNNEL

  @Override
  protected boolean sendTunnelMessage(TunnelMessage cmsg) {

    boolean result = true;

    final Packet packet = Packer.pack(cmsg.toProtocolMessage());

    for (Node node : this.getNodes(cmsg.getTarget())) {
      Connection con = this.getConnection(node.getId());
      if (con != null && con.isActive()) {
        con.writeAndFlushFast(packet);
      } else {
        result = false;
      }
    }

    return result;
  }

  @Override
  protected boolean registerTunnel(Tunnel tunnel) {
    try (CloseableLock l = connectionLock.readLock().open()) {
      this.connections.values().forEach(tunnel::register);
    }
    return true;
  }



}
