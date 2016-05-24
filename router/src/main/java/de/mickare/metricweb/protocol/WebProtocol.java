package de.mickare.metricweb.protocol;

import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;

public abstract class WebProtocol {

  public static interface PacketData {
    default PacketMessage createMessage() {
      Packet packet = this.getClass().getAnnotation(Packet.class);
      return new PacketMessage(packet.name(), this);
    }
  }

  public static @Data @AllArgsConstructor class PacketMessage {
    private @NonNull final String name;
    private final PacketData data;
  }

  // *************************

  private final ConcurrentMap<String, RegisteredPacket<?>> packets = Maps.newConcurrentMap();

  public static class RegisteredPacket<T extends PacketData> {
    private @Getter final String name;
    private @Getter final Class<T> packetClass;

    private RegisteredPacket(String name, Class<T> packetClass) {
      this.name = name;
      this.packetClass = packetClass;
    }

    public boolean isApplicable(Object obj) {
      return packetClass.isInstance(obj);
    }

    public T cast(Object obj) {
      return packetClass.cast(obj);
    }
  }

  public synchronized <T extends PacketData> RegisteredPacket<T> register(
      final Class<T> packetClass) {
    final Packet packet = packetClass.getAnnotation(Packet.class);
    if (packet == null) {
      throw new IllegalArgumentException(
          packetClass.getName() + " is missing the annotation " + Packet.class.getName());
    }
    if (packet.name() == null || packet.name().isEmpty()) {
      throw new IllegalArgumentException("Packet key is missing or empty");
    }
    final String name = packet.name().toLowerCase();
    if (packets.containsKey(name) && !packetClass.equals(packets.get(name))) {
      throw new IllegalStateException(
          "Packet " + packets.get(name) + " already registered for " + name);
    }

    RegisteredPacket<T> reg = new RegisteredPacket<>(name, packetClass);

    packets.put(name, reg);
    return reg;
  }

  public <T extends PacketData> RegisteredPacket<T> getRegisteredPacket(Class<T> packetClass) {
    Packet packet = packetClass.getAnnotation(Packet.class);
    if (packet != null && packet.name() != null) {
      return getRegisteredPacket(packet.name(), packetClass);
    }
    return null;
  }


  public RegisteredPacket<?> getRegisteredPacket(String name) {
    return packets.get(name.toLowerCase());
  }

  @SuppressWarnings("unchecked")
  public <T extends PacketData> RegisteredPacket<T> getRegisteredPacket(String name,
      Class<T> packetClass) {
    RegisteredPacket<?> reg = packets.get(name.toLowerCase());
    if (reg.packetClass.equals(packetClass)) {
      return (RegisteredPacket<T>) reg;
    }
    return null;
  }

  // *************************

  protected abstract void init();

  private class PacketDeserializer implements JsonDeserializer<PacketMessage> {

    @Override
    public PacketMessage deserialize(JsonElement element, Type type,
        JsonDeserializationContext context) throws JsonParseException {
      JsonObject jsonObject = element.getAsJsonObject();

      JsonElement keyElement = jsonObject.get("name");
      if (keyElement == null) {
        throw new JsonParseException("name is missing");
      }

      final String name = keyElement.getAsString().toLowerCase();
      RegisteredPacket<?> regPacket = packets.get(name);
      if (regPacket == null) {
        throw new JsonParseException("packet \"" + name + "\" unknown");
      }

      return new PacketMessage(name,
          context.deserialize(jsonObject.get("data").getAsJsonObject(), regPacket.packetClass));
    }

  }

  private final Gson GSON = new GsonBuilder().serializeNulls() //
      .registerTypeHierarchyAdapter(byte[].class, new JsonTools.ByteArrayToBase64TypeAdapter()) //
      .registerTypeHierarchyAdapter(UUID.class, new JsonTools.UUIDTypeAdapter()) //
      .registerTypeAdapter(PacketMessage.class, new PacketDeserializer()).create();

  public PacketMessage decode(String json) {
    return GSON.fromJson(json, PacketMessage.class);
  }

  public String encode(PacketMessage message) {
    return GSON.toJson(message, PacketMessage.class);
  }



}
