package de.rennschnitzel.net.event;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.Connection;
import lombok.Getter;

public class ConnectionEvent extends NetworkEvent {


  @Getter
  private final Connection connection;

  public ConnectionEvent(Connection connection) {
    super(connection.getNetwork());
    Preconditions.checkNotNull(connection);
    this.connection = connection;
  }
  


}
