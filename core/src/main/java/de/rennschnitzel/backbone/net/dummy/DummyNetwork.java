package de.rennschnitzel.backbone.net.dummy;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.ProtocolUtils;
import de.rennschnitzel.backbone.net.Connection;
import de.rennschnitzel.backbone.net.AbstractNetwork;
import de.rennschnitzel.backbone.net.Node.HomeNode;
import de.rennschnitzel.backbone.net.ProcedureManager;
import de.rennschnitzel.backbone.net.Target;
import de.rennschnitzel.backbone.net.procedure.ProcedureCall;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.NodeMessage.Type;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.NodeUpdateMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.Packet;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureResponseMessage;
import de.rennschnitzel.backbone.util.concurrent.DirectScheduledExecutorService;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class DummyNetwork extends AbstractNetwork {

  @Getter
  @Setter
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

  @Override
  public <T, R> void sendProcedureCall(ProcedureCall<T, R> call) {
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
  public void sendProcedureResponse(UUID receiver, ProcedureResponseMessage msg) {
    connection.send(Packet.newBuilder().setProcedureMessage(ProcedureMessage.newBuilder()
        .setSender(ProtocolUtils.convert(getHome().getId())).setTarget(Target.to(receiver).getProtocolMessage()).setResponse(msg)));
  }

  @Override
  public void scheduleAsyncLater(Runnable run, long timeout, TimeUnit unit) {
    executor.schedule(run, timeout, unit);
  }

  @Override
  public void sendHomeNodeUpdate() {
    this.connection.send(Packet.newBuilder().setNodeUpdate(NodeUpdateMessage.newBuilder().setNode(this.getHome().toProtocol())));
  }

}
