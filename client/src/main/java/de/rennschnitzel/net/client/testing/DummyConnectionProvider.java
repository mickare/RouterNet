package de.rennschnitzel.net.client.testing;

import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.AbstractScheduledService;

import de.rennschnitzel.net.client.connection.ConnectionService;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.exception.NotConnectedException;

public class DummyConnectionProvider extends AbstractScheduledService
    implements ConnectionService {

  public DummyConnectionProvider() {
    // TODO Auto-generated constructor stub
  }

  @Override
  public Connection getConnection(long time, TimeUnit unit) throws NotConnectedException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected void runOneIteration() throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedDelaySchedule(1, 50, TimeUnit.MILLISECONDS);
  }

}
