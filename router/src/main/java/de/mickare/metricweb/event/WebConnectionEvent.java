package de.mickare.metricweb.event;

import de.mickare.metricweb.websocket.WebConnection;
import lombok.Data;
import lombok.NonNull;

public abstract @Data class WebConnectionEvent {

  private @NonNull final WebConnection connection;

}
