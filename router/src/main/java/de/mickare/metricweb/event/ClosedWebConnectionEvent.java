package de.mickare.metricweb.event;

import de.mickare.metricweb.websocket.WebConnection;

public class ClosedWebConnectionEvent extends WebConnectionEvent {

  public ClosedWebConnectionEvent(WebConnection connection) {
    super(connection);
  }

}
