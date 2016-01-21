package de.rennschnitzel.net.netty;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service.State;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NettyClient2 {

  private final ClientService service = new ClientService();
  private final HostAndPort address;



  protected Executor executor() {
    return MoreExecutors.directExecutor();
  }

  public State state() {
    return service.state();
  }

  public void connect() {
    service.startAsync();
  }

  public void awaitRunning() {}

  public void awaitRunning(long timeout, TimeUnit unit) {}

  public void awaitTerminated() {}

  public void awaitTerminated(long timeout, TimeUnit unit) {}

  public Throwable failureCause() {
    return this.service.failureCause();
  }

  public boolean isConnected() {
    return this.service.isRunning();
  }


  private class ClientService extends AbstractIdleService {

    @Override
    protected void startUp() throws Exception {
      // TODO Auto-generated method stub

    }

    @Override
    protected void shutDown() throws Exception {
      // TODO Auto-generated method stub

    }

    @Override
    protected Executor executor() {
      return NettyClient2.this.executor();
    }
  }

}
