package de.rennschnitzel.net;

import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.client.NetClient;
import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.procedure.ProcedureCall;
import de.rennschnitzel.net.exception.NotConnectedException;
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
    return null;
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
  protected <T, R> void sendProcedureCall(ProcedureCall<T, R> call) {
    // TODO Auto-generated method stub

  }

  @Override
  protected void sendProcedureResponse(UUID receiver, ProcedureResponseMessage build) {
    // TODO Auto-generated method stub

  }

  @Override
  protected void sendHomeNodeUpdate() {
    // TODO Auto-generated method stub

  }


}
