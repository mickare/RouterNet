package de.rennschnitzel.backbone.net;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.net.node.HomeNode;
import de.rennschnitzel.backbone.net.procedure.ProcedureCall;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.ServerUpdateMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.Packet;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureResponseMessage;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class NetworkForTesting extends Network {

  @Getter
  @Setter
  @NonNull
  private Connection connection = null;

  @Getter
  private final ProcedureManager procedureManager = new ProcedureManager(this);


  public NetworkForTesting() {
    this(new HomeNode(UUID.randomUUID()));
  }

  public NetworkForTesting(HomeNode home) {
    super(home);
  }

  public void setInstance() {
    super.setInstance();
  }

  @Override
  public <T, R> void sendProcedureCall(ProcedureCall<T, R> call) {
    ProcedureMessage.Builder b = ProcedureMessage.newBuilder();
    b.setTarget(call.getTarget().getProtocolMessage());
    b.setSender(getHome().getIdProto());
    b.setCall(call.toProtocol());
    connection.send(Packet.newBuilder().setProcedureMessage(b));
  }

  @Override
  public Logger getLogger() {
    return Logger.getLogger("NetworkForTesting");
  }

  @Override
  public void sendProcedureResponse(UUID receiver, ProcedureResponseMessage msg) {
    connection.send(Packet.newBuilder().setProcedureMessage(ProcedureMessage.newBuilder().setSender(this.getHome().getIdProto())
        .setTarget(Target.to(receiver).getProtocolMessage()).setResponse(msg)));
  }

  @Override
  public void scheduleAsyncLater(Runnable run, long timeout, TimeUnit unit) {
    run.run();
  }

  @Override
  public void publishChanges(HomeNode homeNode) {
    Preconditions.checkNotNull(homeNode);
    Preconditions.checkNotNull(this.connection);
    this.connection.send(Packet.newBuilder().setUpdate(ServerUpdateMessage.newBuilder().setServer(homeNode.toProtocol())));
  }

}
