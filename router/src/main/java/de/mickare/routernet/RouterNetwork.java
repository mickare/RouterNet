package de.mickare.routernet;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import de.mickare.routernet.ProtocolUtils;
import de.mickare.routernet.core.AbstractNetwork;
import de.mickare.routernet.core.Connection;
import de.mickare.routernet.core.Node;
import de.mickare.routernet.core.NotConnectedException;
import de.mickare.routernet.core.Target;
import de.mickare.routernet.core.Tunnel;
import de.mickare.routernet.core.Node.HomeNode;
import de.mickare.routernet.core.packet.Packer;
import de.mickare.routernet.core.procedure.ProcedureCall;
import de.mickare.routernet.core.tunnel.TunnelMessage;
import de.mickare.routernet.event.ConnectionAddedEvent;
import de.mickare.routernet.event.ConnectionRemovedEvent;
import de.mickare.routernet.exception.ProtocolException;
import de.mickare.routernet.protocol.NetworkProtocol.NodeMessage;
import de.mickare.routernet.protocol.NetworkProtocol.NodeRemoveMessage;
import de.mickare.routernet.protocol.NetworkProtocol.NodeUpdateMessage;
import de.mickare.routernet.protocol.TransportProtocol.Packet;
import de.mickare.routernet.protocol.TransportProtocol.ProcedureMessage;
import de.mickare.routernet.protocol.TransportProtocol.ProcedureResponseMessage;
import de.mickare.routernet.router.Router;
import de.mickare.routernet.util.concurrent.CloseableLock;
import de.mickare.routernet.util.concurrent.CloseableReadWriteLock;
import de.mickare.routernet.util.concurrent.ReentrantCloseableReadWriteLock;

public class RouterNetwork extends AbstractNetwork {

  private final Router router;

  private final CloseableReadWriteLock connectionLock = new ReentrantCloseableReadWriteLock(true);
  private final Map<UUID, Connection> connections = new ConcurrentHashMap<>();

  public RouterNetwork(Router router, ScheduledExecutorService scheduler, HomeNode home) {
    super(router.getLogger(), scheduler, home);
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
        this.getEventBus().post(new ConnectionRemovedEvent(connection));
      }
      String name = connection.getName();
      getLogger()
          .info(connection.getPeerId() + (name != null ? "(" + name + ")" : "") + " connected.");
      this.getEventBus().post(new ConnectionAddedEvent(connection));
    }
  }

  @Override
  protected void removeConnection(Connection connection) {
    try (CloseableLock l = connectionLock.writeLock().open()) {
      if (this.connections.remove(connection.getPeerId(), connection)) {
        String name = connection.getName();
        getLogger().info(
            connection.getPeerId() + (name != null ? "(" + name + ")" : "") + " disconnected.");
        this.getEventBus().post(new ConnectionRemovedEvent(connection));
        this.removeNode(connection.getPeerId());
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

  @Override
  public boolean isConnected() {
    // Always connected!
    return true;
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

  @Override
  public Node updateNode(Connection con, NodeMessage msg) {
    UUID nodeId = ProtocolUtils.convert(msg.getId());
    Preconditions.checkArgument(con.getPeerId().equals(nodeId));
    Node node = super.updateNode(con, msg);
    forwardNodeUpdate(con, NodeUpdateMessage.newBuilder().setNode(msg));
    return node;
  }

  private void forwardNodeUpdate(Connection in, NodeUpdateMessage.Builder msg) {
    final Packet packet = Packer.pack(msg);
    final UUID inId = in.getPeerId();
    try (CloseableLock l = connectionLock.readLock().open()) {
      this.connections.values().stream().filter(con -> !con.getPeerId().equals(inId))
          .forEach(out -> {
            out.writeAndFlushFast(packet);
          });
    }
  }

  @Override
  public void removeNode(UUID id) {
    Preconditions.checkNotNull(id);
    final Packet packet =
        Packer.pack(NodeRemoveMessage.newBuilder().setId(ProtocolUtils.convert(id)));
    try (CloseableLock l = connectionLock.readLock().open()) {
      if (!this.connections.containsKey(id)) {
        super.removeNode(id);
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
      this.connections.values().forEach(tunnel::sendTunnelRegister);
    }
    return true;
  }


}
