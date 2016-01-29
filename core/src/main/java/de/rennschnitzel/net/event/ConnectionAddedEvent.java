package de.rennschnitzel.net.event;

import de.rennschnitzel.net.core.Connection;

public class ConnectionAddedEvent extends ConnectionEvent {

  public ConnectionAddedEvent(Connection connection) {
    super(connection);
  }
}
