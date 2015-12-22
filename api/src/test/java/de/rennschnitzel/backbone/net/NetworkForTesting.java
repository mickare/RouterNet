package de.rennschnitzel.backbone.net;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.net.procedure.ProcedureCall;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.Packet;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureMessage;

public class NetworkForTesting extends Network {

  private final Connection connection;

  public NetworkForTesting(Connection connection) {
    super(connection.getHome());
    Preconditions.checkNotNull(connection);
    this.connection = connection;
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
  public ProcedureManager getProcedureManager() {
    // TODO Auto-generated method stub
    return null;
  }

}
