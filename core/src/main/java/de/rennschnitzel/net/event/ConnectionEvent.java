package de.rennschnitzel.net.event;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.Connection;
import lombok.Getter;

public class ConnectionEvent extends NetworkEvent {


  @Getter
  private final Connection connection;

  public ConnectionEvent(AbstractNetwork network, Connection connection) {
    super(network);
    Preconditions.checkNotNull(connection);
    this.connection = connection;
  }

  public static class OpenConnectionEvent extends ConnectionEvent {

    public OpenConnectionEvent(AbstractNetwork network, Connection connection) {
      super(network, connection);
    }
  }

  public static class ClosedConnectionEvent extends ConnectionEvent {

    public ClosedConnectionEvent(AbstractNetwork network, Connection connection) {
      super(network, connection);
    }
  }

}
