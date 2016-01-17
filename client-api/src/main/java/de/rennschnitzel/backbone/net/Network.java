package de.rennschnitzel.backbone.net;

import de.rennschnitzel.backbone.exception.NotConnectedException;
import de.rennschnitzel.backbone.net.Node.HomeNode;

public abstract class Network extends AbstractNetwork {

  protected Network(HomeNode home) {
    super(home);
  }

  public abstract Connection getConnection() throws NotConnectedException;


}
