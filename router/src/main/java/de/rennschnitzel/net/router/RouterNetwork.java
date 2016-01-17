package de.rennschnitzel.net.router;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.HomeNode;
import de.rennschnitzel.net.core.procedure.ProcedureCall;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureResponseMessage;

public class RouterNetwork extends AbstractNetwork {

  private final Router router;

  public RouterNetwork(Router router, HomeNode home) {
    super(home);
    Preconditions.checkNotNull(router);
    this.router = router;
  }

  @Override
  public Logger getLogger() {
    return router.getLogger();
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
  public void publishChanges(HomeNode homeNode) {
    // TODO Auto-generated method stub

  }

  @Override
  public void scheduleAsyncLater(Runnable run, long timeout, TimeUnit unit) {
    // TODO Auto-generated method stub

  }


}
