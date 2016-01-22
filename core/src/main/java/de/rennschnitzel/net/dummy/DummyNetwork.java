package de.rennschnitzel.net.dummy;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import de.rennschnitzel.net.ProtocolUtils;
import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.Node.HomeNode;
import de.rennschnitzel.net.core.ProcedureManager;
import de.rennschnitzel.net.core.Target;
import de.rennschnitzel.net.core.Tunnel;
import de.rennschnitzel.net.core.procedure.ProcedureCall;
import de.rennschnitzel.net.core.tunnel.TunnelMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeMessage.Type;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeUpdateMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureResponseMessage;
import de.rennschnitzel.net.util.FutureUtils;
import de.rennschnitzel.net.util.concurrent.DirectScheduledExecutorService;
import io.netty.util.concurrent.Future;
import lombok.Getter;

public class DummyNetwork extends AbstractNetwork {

  private static Logger LOGGER_DEFAULT = new DummyLogger("DummyNetwork", System.out);

  @Getter
  private Connection connection = null;

  @Getter
  private final ProcedureManager procedureManager = new ProcedureManager(this);

  private final ScheduledExecutorService executor;

  @Getter
  private Logger logger = LOGGER_DEFAULT;

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

  public void setName(String name) {
    this.logger = new DummyLogger(name, System.out);
  }

  public synchronized boolean addConnection0(Connection connection) {
    Preconditions.checkArgument(connection.getNetwork() == this);
    this.connection = connection;
    return true;
  }

  public synchronized boolean removeConnection0(Connection connection) {
    if (this.connection == connection) {
      this.connection = null;
      return true;
    }
    return false;
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

      if (!call.getTarget().isOnly(this.getHome())) {
        connection.send(packet);
      }

      if (call.getTarget().contains(this.getHome())) {
        this.procedureManager.handle(call);
      }

    } catch (Exception e) {
      return FutureUtils.futureFailure(e);
    }

    return FutureUtils.SUCCESS;
  }

  @Override
  public Future<?> sendProcedureResponse(UUID receiver, ProcedureResponseMessage msg) {
    ProcedureMessage pmsg = ProcedureMessage.newBuilder().setSender(ProtocolUtils.convert(getHome().getId()))
        .setTarget(Target.to(receiver).getProtocolMessage()).setResponse(msg).build();
    if (this.getHome().getId().equals(receiver)) {
      try {
        this.procedureManager.handle(connection, pmsg);
        return FutureUtils.SUCCESS;
      } catch (Exception e) {
        return FutureUtils.futureFailure(e);
      }
    } else {
      return connection.send(Packet.newBuilder().setProcedureMessage(pmsg));
    }
  }

  @Override
  public ScheduledFuture<?> scheduleAsyncLater(Runnable run, long timeout, TimeUnit unit) {
    return executor.schedule(run, timeout, unit);
  }

  @Override
  public void publishHomeNodeUpdate() {
    this.getHome().publishUpdate(Sets.newHashSet(this.connection));
  }

  @Override
  protected Future<?> sendTunnelMessage(TunnelMessage cmsg) {
    try {
      return connection.send(Packet.newBuilder().setTunnelMessage(cmsg.toProtocolMessage(connection)));
    } catch (IOException e) {
      return FutureUtils.futureFailure(e);
    }
  }

  @Override
  protected Future<?> registerTunnel(Tunnel tunnel) {
    return FutureUtils.transformFuture(connection.registerTunnel(tunnel));
  }

}
