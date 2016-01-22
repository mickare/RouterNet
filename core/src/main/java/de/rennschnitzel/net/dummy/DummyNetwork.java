package de.rennschnitzel.net.dummy;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.protocol.NetworkProtocol.NodeMessage.Type;
import de.rennschnitzel.net.ProtocolUtils;
import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.ProcedureManager;
import de.rennschnitzel.net.core.Target;
import de.rennschnitzel.net.core.Tunnel;
import de.rennschnitzel.net.core.Node.HomeNode;
import de.rennschnitzel.net.core.procedure.ProcedureCall;
import de.rennschnitzel.net.core.tunnel.TunnelMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeUpdateMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureResponseMessage;
import de.rennschnitzel.net.util.concurrent.DirectScheduledExecutorService;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class DummyNetwork extends AbstractNetwork {

  @Getter
  @NonNull
  private Connection connection = null;

  @Getter
  private final ProcedureManager procedureManager = new ProcedureManager(this);

  private final ScheduledExecutorService executor;

  public DummyNetwork() {
    this(new DirectScheduledExecutorService());
  }

  public DummyNetwork(HomeNode home) {
    this(new DirectScheduledExecutorService(), home);
  }

  public DummyNetwork(ScheduledExecutorService executor) {
    this(executor, new HomeNode(UUID.randomUUID()));
  }

  public DummyNetwork(ScheduledExecutorService executor, HomeNode home) {
    super(home);
    Preconditions.checkNotNull(executor);
    this.executor = executor;
    home.setType(Type.BUKKIT);
  }

  public synchronized void addConnection0(Connection connection) {
    Preconditions.checkArgument(connection.getNetwork() == this);
    this.connection = connection;
  }

  public synchronized void removeConnection(Connection connection) {
    if (this.connection == connection) {
      this.connection = null;
    }
  }

  @Override
  public <T, R> void sendProcedureCall(ProcedureCall<T, R> call) throws IOException {
    ProcedureMessage.Builder b = ProcedureMessage.newBuilder();
    b.setTarget(call.getTarget().getProtocolMessage());
    b.setSender(ProtocolUtils.convert(getHome().getId()));
    b.setCall(call.toProtocol());
    connection.send(Packet.newBuilder().setProcedureMessage(b));
  }

  @Override
  public Logger getLogger() {
    return Logger.getLogger("NetworkForTesting");
  }

  @Override
  public void sendProcedureResponse(UUID receiver, ProcedureResponseMessage msg) throws IOException {
    connection.send(Packet.newBuilder().setProcedureMessage(ProcedureMessage.newBuilder()
        .setSender(ProtocolUtils.convert(getHome().getId())).setTarget(Target.to(receiver).getProtocolMessage()).setResponse(msg)));
  }

  @Override
  public ScheduledFuture<?> scheduleAsyncLater(Runnable run, long timeout, TimeUnit unit) {
    return executor.schedule(run, timeout, unit);
  }

  @Override
  public void sendHomeNodeUpdate() throws IOException {
    connection.send(Packet.newBuilder().setNodeUpdate(NodeUpdateMessage.newBuilder().setNode(this.getHome().toProtocol())));
  }

  @Override
  protected void sendTunnelMessage(TunnelMessage cmsg) throws IOException {
    connection.send(Packet.newBuilder().setTunnelMessage(cmsg.toProtocolMessage(connection)));
  }

  @Override
  protected void registerTunnel(Tunnel tunnel) throws IOException {
    connection.registerTunnel(tunnel);
  }

}
