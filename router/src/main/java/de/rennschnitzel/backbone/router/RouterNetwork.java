package de.rennschnitzel.backbone.router;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import de.rennschnitzel.backbone.net.Network;
import de.rennschnitzel.backbone.net.ProcedureManager;
import de.rennschnitzel.backbone.net.node.HomeNode;
import de.rennschnitzel.backbone.net.procedure.ProcedureCall;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureResponseMessage;

public class RouterNetwork extends Network {

  public RouterNetwork(HomeNode home) {
    super(home);
    // TODO Auto-generated constructor stub
  }

  @Override
  public Logger getLogger() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <T, R> void sendProcedureCall(ProcedureCall<T, R> call) {
    // TODO Auto-generated method stub

  }

  @Override
  public void sendProcedureResponse(UUID receiver, ProcedureResponseMessage build) {
    // TODO Auto-generated method stub

  }

  @Override
  public ProcedureManager getProcedureManager() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void publishChanges(HomeNode homeNode) {
    // TODO Auto-generated method stub

  }

  @Override
  public void scheduleAsyncLater(Runnable run, long timeout, TimeUnit unit) {
    // TODO Auto-generated method stub

  }

}
