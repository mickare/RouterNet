package de.rennschnitzel.backbone.client.util;

import java.util.UUID;

import de.rennschnitzel.backbone.ProtocolUtils;
import de.rennschnitzel.backbone.net.protocol.ComponentUUID;
import de.rennschnitzel.backbone.util.FutureContainer;

public class UUIDContainer extends FutureContainer<UUID> {

  public static UUID of(ComponentUUID.UUID uuid) {
    return ProtocolUtils.convert(uuid);
  }

}
