package de.rennschnitzel.backbone;

import java.util.UUID;

import de.rennschnitzel.backbone.net.protocol.ComponentUUID;

public class ProtocolUtils {

  public static UUID convert(ComponentUUID.UUID uuid) {
    return new UUID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
  }

  public static ComponentUUID.UUID convert(UUID uuid) {
    return ComponentUUID.UUID.newBuilder().setMostSignificantBits(uuid.getMostSignificantBits())
        .setLeastSignificantBits(uuid.getLeastSignificantBits()).build();
  }

}
