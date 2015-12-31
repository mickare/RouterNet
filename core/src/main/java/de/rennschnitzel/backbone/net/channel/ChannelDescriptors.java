package de.rennschnitzel.backbone.net.channel;

import java.io.InvalidClassException;
import java.io.Serializable;

import de.rennschnitzel.backbone.net.channel.object.ObjectChannel;
import de.rennschnitzel.backbone.net.channel.object.ObjectConverters;
import de.rennschnitzel.backbone.net.channel.stream.StreamChannel;

public class ChannelDescriptors {

  private ChannelDescriptors() {}

  public static ObjectChannel.Descriptor<byte[]> getByteChannel(String name) {
    return new ObjectChannel.Descriptor<>(name, byte[].class, ObjectConverters.BYTE_ARRAY);
  }

  public static <T extends Serializable> ObjectChannel.Descriptor<T> getObjectChannel(String name, Class<T> dataClass) {
    try {
      return new ObjectChannel.Descriptor<>(name, dataClass);
    } catch (InvalidClassException e) {
      // should not happen
      throw new RuntimeException(e);
    }
  }

  public static StreamChannel.Descriptor getStreamChannel(String name) {
    return new StreamChannel.Descriptor(name);
  }

  public static StreamChannel.Descriptor getStreamChannel(String name, int bufferSize) {
    return new StreamChannel.Descriptor(name, bufferSize);
  }


}
