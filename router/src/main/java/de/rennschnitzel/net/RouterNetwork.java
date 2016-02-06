package de.rennschnitzel.net;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

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
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeRemoveMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeUpdateMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureResponseMessage;
import de.rennschnitzel.net.router.Router;
import de.rennschnitzel.net.util.concurrent.CloseableLock;
import de.rennschnitzel.net.util.concurrent.CloseableReadWriteLock;
import de.rennschnitzel.net.util.concurrent.ReentrantCloseableReadWriteLock;

public class RouterNetwork extends AbstractNetwork {

  private final Router router;

  private final CloseableReadWriteLock connectionLock = new ReentrantCloseableReadWriteLock(true);
  private final Map<UUID, Connection> connections = new ConcurrentHashMap<>();

  public RouterNetwork(Router router, HomeNode home) {
    super(router.getScheduler(), home);
    Preconditions.checkNotNull(router);
    this.router = router;
    Net.setNetwork(this);
  }

  @Override
  public Logger getLogger() {
    return router.getLogger();
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
      String name = connection.getName();
      getLogger()
          .info(connection.getPeerId() + (name != null ? "(" + name + ")" : "") + " connected.");
    }
  }

  @Override
  protected void removeConnection(Connection connection) {
    try (CloseableLock l = connectionLock.writeLock().open()) {
      if (this.connections.remove(connection.getPeerId(), connection)) {
        String name = connection.getName();
        getLogger().info(
            connection.getPeerId() + (name != null ? "(" + name + ")" : "") + " disconnected.");
        this.removeNode(connection.getPeerId());
        publishNodeRemove(connection.getPeerId());
      }
      if (connection.isActive()) {
        connection.getChannel().close();
      }
    }
  }

  public Connection getConnection(UUID peerId) {
    try (CloseableLock l = connectionLock.readLock().open()) {
      return this.connections.get(peerId);
    }
  }

  public List<Connection> getConnections() {
    try (CloseableLock l = connectionLock.readLock().open()) {
      return Lists.newArrayList(this.connections.values());
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
  // NODE
  public void forwardNodeUpdate(Connection in, NodeUpdateMessage msg) {
    final Packet packet = Packer.pack(msg);
    final UUID inId = in.getPeerId();
    try (CloseableLock l = connectionLock.readLock().open()) {
      this.connections.values().stream().filter(con -> !con.getPeerId().equals(inId))
          .forEach(out -> {
            out.writeAndFlushFast(packet);
          });
    }
  }

  private void publishNodeRemove(UUID id) {
    Preconditions.checkNotNull(id);
    final Packet packet =
        Packer.pack(NodeRemoveMessage.newBuilder().setId(ProtocolUtils.convert(id)));
    try (CloseableLock l = connectionLock.readLock().open()) {
      if (!this.connections.containsKey(id)) {
        this.connections.values().forEach(con -> {
          con.writeAndFlushFast(packet);
        });
      }
    }
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
