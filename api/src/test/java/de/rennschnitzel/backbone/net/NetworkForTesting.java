package de.rennschnitzel.backbone.net;

import java.util.UUID;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.net.node.HomeNode;
import de.rennschnitzel.backbone.net.procedure.ProcedureCall;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.Packet;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureResponseMessage;
import de.rennschnitzel.backbone.net.store.DataStore;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class NetworkForTesting extends Network {

  @Getter
  @Setter
  @NonNull
  private Connection connection = null;

  @Getter
  private final DataStore dataStore;

  @Getter
  private final ProcedureManager procedureManager = new ProcedureManager(this);

  public NetworkForTesting(DataStore dataStore) {
    Preconditions.checkNotNull(dataStore);
    this.dataStore = dataStore;
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
  public HomeNode getHome() {
    return connection.getHome();
  }

  @Override
  public Logger getLogger() {
    return Logger.getLogger("NetworkForTesting");
  }

  @Override
  public void sendProcedureResponse(UUID receiver, ProcedureResponseMessage msg) {
    connection.send(Packet.newBuilder()
        .setProcedureMessage(ProcedureMessage.newBuilder().setTarget(Target.to(receiver).getProtocolMessage()).setResponse(msg)));
  }

}
