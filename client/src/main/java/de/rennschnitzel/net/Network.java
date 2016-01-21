package de.rennschnitzel.net;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.Target;
import de.rennschnitzel.net.core.procedure.ProcedureCall;
import de.rennschnitzel.net.exception.NotConnectedException;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeUpdateMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureResponseMessage;
import lombok.Getter;

public class Network extends AbstractNetwork {

  @Getter
  private final NetClient client;

  public Network(NetClient client) {
    super(client.getHome());
    Preconditions.checkNotNull(client);
    this.client = client;
  }

  public Connection getConnection() throws NotConnectedException {
    return client.getConnectionService().getConnection(1, TimeUnit.SECONDS);
  }

  @Override
  public Logger getLogger() {
    return client.getLogger();
  }

  @Override
  public ScheduledFuture<?> scheduleAsyncLater(Runnable run, long timeout, TimeUnit unit) {
    return client.getExecutor().schedule(run, timeout, unit);
  }

  @Override
  public <T, R> void sendProcedureCall(ProcedureCall<T, R> call) throws IOException {
    ProcedureMessage.Builder b = ProcedureMessage.newBuilder();
    b.setTarget(call.getTarget().getProtocolMessage());
    b.setSender(ProtocolUtils.convert(getHome().getId()));
    b.setCall(call.toProtocol());

    getConnection().send(Packet.newBuilder().setProcedureMessage(b));
  }

  @Override
  public void sendProcedureResponse(UUID receiver, ProcedureResponseMessage msg)
      throws IOException {
    getConnection().send(Packet.newBuilder().setProcedureMessage(
        ProcedureMessage.newBuilder().setSender(ProtocolUtils.convert(getHome().getId()))
            .setTarget(Target.to(receiver).getProtocolMessage()).setResponse(msg)));
  }

  @Override
  public void sendHomeNodeUpdate() throws IOException {
    getConnection().send(Packet.newBuilder()
        .setNodeUpdate(NodeUpdateMessage.newBuilder().setNode(this.getHome().toProtocol())));
  }


}
