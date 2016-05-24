package de.mickare.metricweb.event;

import de.mickare.metricweb.websocket.WebConnection;

public class OpenedWebConnectionEvent extends WebConnectionEvent {

  public OpenedWebConnectionEvent(WebConnection connection) {
    super(connection);
  }


}
