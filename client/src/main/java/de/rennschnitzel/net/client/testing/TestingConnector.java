package de.rennschnitzel.net.client.testing;

import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.netty.ConnectionFuture;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;

public interface TestingConnector<C extends Connection> extends ConnectionFuture<C> {
  void disconnect(CloseMessage msg);
}
