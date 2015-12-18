package de.rennschnitzel.backbone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import de.rennschnitzel.backbone.net.protocol.ComponentUUID;

public class ProtocolUtils {

  public static UUID convert(final ComponentUUID.UUID uuid) {
    if (uuid == null) {
      return null;
    }
    return new UUID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
  }

  public static ComponentUUID.UUID convert(final UUID uuid) {
    if (uuid == null) {
      return null;
    }
    return ComponentUUID.UUID.newBuilder().setMostSignificantBits(uuid.getMostSignificantBits())
        .setLeastSignificantBits(uuid.getLeastSignificantBits()).build();
  }

  public static List<UUID> convertProto(final List<ComponentUUID.UUID> c) {
    if (c == null) {
      return null;
    }
    final List<UUID> result = new ArrayList<>(c.size());
    for (int i = 0; i < c.size(); ++i) {
      result.add(convert(c.get(i)));
    }
    return result;
  }

  public static List<ComponentUUID.UUID> convert(final List<UUID> c) {
    if (c == null) {
      return null;
    }
    final List<ComponentUUID.UUID> result = new ArrayList<>(c.size());
    for (int i = 0; i < c.size(); ++i) {
      result.add(convert(c.get(i)));
    }
    return result;
  }


  // private static final Function<UUID, ComponentUUID.UUID> toProtConv

  public static Collection<UUID> convertProto(Collection<ComponentUUID.UUID> c) {
    if (c == null) {
      return null;
    }
    final List<UUID> result = new ArrayList<>(c.size());
    c.forEach(old -> result.add(convert(old)));
    return result;
  }


  public static Collection<ComponentUUID.UUID> convert(Collection<UUID> c) {
    if (c == null) {
      return null;
    }
    final List<ComponentUUID.UUID> result = new ArrayList<>(c.size());
    c.forEach(old -> result.add(convert(old)));
    return result;
  }

}
