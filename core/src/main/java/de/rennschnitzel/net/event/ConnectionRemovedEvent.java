package de.rennschnitzel.net.event;

import de.rennschnitzel.net.core.Connection;

public class ConnectionRemovedEvent extends ConnectionEvent {

  public ConnectionRemovedEvent(Connection connection) {
    super(connection);
  }
}
