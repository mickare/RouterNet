package de.rennschnitzel.backbone.event;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.net.Connection;
import de.rennschnitzel.backbone.net.Network;
import lombok.Getter;

public class ConnectionEvent extends NetworkEvent {


  @Getter
  private final Connection connection;

  public ConnectionEvent(Network network, Connection connection) {
    super(network);
    Preconditions.checkNotNull(connection);
    this.connection = connection;
  }

  public static class OpenConnectionEvent extends ConnectionEvent {

    public OpenConnectionEvent(Network network, Connection connection) {
      super(network, connection);
    }
  }

  public static class ClosedConnectionEvent extends ConnectionEvent {

    public ClosedConnectionEvent(Network network, Connection connection) {
      super(network, connection);
    }
  }

}
