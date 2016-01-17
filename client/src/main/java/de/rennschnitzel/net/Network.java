package de.rennschnitzel.net;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.Node.HomeNode;
import de.rennschnitzel.net.exception.NotConnectedException;

public abstract class Network extends AbstractNetwork {

  protected Network(HomeNode home) {
    super(home);
  }

  public abstract Connection getConnection() throws NotConnectedException;


}
