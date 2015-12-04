package de.rennschnitzel.backbone.client.util;

import java.util.UUID;

import com.google.common.util.concurrent.ListenableFuture;

import de.rennschnitzel.backbone.api.network.Connection;
import de.rennschnitzel.backbone.net.protocol.ComponentUUID;
import de.rennschnitzel.backbone.util.FutureContainer;

public class UUIDContainer extends FutureContainer<UUID> {

  public static UUID of(ComponentUUID.UUID packet) {
    return new UUID(packet.getMostSignificantBits(), packet.getLeastSignificantBits());
  }
  
  public boolean setSuccess(ListenableFuture<? extends Connection> delegate) {
    return set(delegate, Connection::getClientUUID);
  }

}
