package de.rennschnitzel.net.client.netty;

import de.rennschnitzel.net.Network;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;

public class ClientConnection extends Connection {

  public ClientConnection(Network network) {
    super(network);
  }

  @Override
  public void send(Packet packet) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public boolean isClosed() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isActive() {
    // TODO Auto-generated method stub
    return false;
  }

}
