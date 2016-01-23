package de.rennschnitzel.net.core;

import de.rennschnitzel.net.protocol.TransportProtocol;
import io.netty.util.concurrent.Future;

public interface PacketOut {

  public abstract Future<?> send(TransportProtocol.Packet packet);

  default Future<?> send(TransportProtocol.Packet.Builder packet) {
    return send(packet.build());
  }

}
