package de.rennschnitzel.net.client.testing;

import de.rennschnitzel.net.client.connection.AbstractConnectionService;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;

public class TestingConnectionService extends AbstractConnectionService<TestingConnector<?>> {


  private final TestingFramework framework;

  public TestingConnectionService(TestingFramework framework) {
    super(framework.getClient().getExecutor());
    this.framework = framework;
  }


  @Override
  protected TestingConnector<?> connect() {
    return framework.connect();
  }

  @Override
  protected void disconnect(TestingConnector<?> con, CloseMessage msg) {
    framework.disconnect(con, msg);
  }


}
