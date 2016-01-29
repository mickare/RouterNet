package de.rennschnitzel.net.router;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.Tunnel;
import de.rennschnitzel.net.core.Node.HomeNode;
import de.rennschnitzel.net.core.procedure.ProcedureCall;
import de.rennschnitzel.net.core.tunnel.TunnelMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureResponseMessage;
import io.netty.util.concurrent.Future;

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
  public ScheduledExecutorService getExecutor() {
    return router.getScheduler();
  }

  @Override
  protected boolean addConnection0(Connection connection) throws Exception {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  protected boolean removeConnection0(Connection connection) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  protected <T, R> Future<?> sendProcedureCall(ProcedureCall<T, R> call) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected Future<?> sendProcedureResponse(UUID receiverId, ProcedureResponseMessage build) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected void publishHomeNodeUpdate() {
    // TODO Auto-generated method stub

  }

  @Override
  protected Future<Integer> registerTunnel(Tunnel tunnel) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected Future<?> sendTunnelMessage(TunnelMessage cmsg) {
    // TODO Auto-generated method stub
    return null;
  }


}
