package de.rennschnitzel.net;

import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

import de.rennschnitzel.net.core.AbstractClientNetwork;
import lombok.Getter;

public class Network extends AbstractClientNetwork {

  @Getter
  private final NetClient client;

  public Network(NetClient client) {
    super(client.getHome());
    this.client = client;
  }

  @Override
  public Logger getLogger() {
    return client.getLogger();
  }

  @Override
  public ScheduledExecutorService getExecutor() {
    return client.getExecutor();
  }

  public void resetInstance() {
    super.setInstance(this);
  }

}
