package de.rennschnitzel.net.core;

import de.rennschnitzel.net.protocol.TransportProtocol;
import io.netty.util.concurrent.Future;

public interface PacketOutWriter {

  public abstract Future<?> send(TransportProtocol.Packet packet);

  default Future<?> send(TransportProtocol.Packet.Builder packet) {
    return send(packet.build());
  }

}
