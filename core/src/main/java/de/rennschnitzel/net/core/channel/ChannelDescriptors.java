package de.rennschnitzel.net.core.channel;

import java.io.InvalidClassException;
import java.io.Serializable;

import de.rennschnitzel.net.core.channel.object.ObjectChannel;
import de.rennschnitzel.net.core.channel.object.ObjectConverters;
import de.rennschnitzel.net.core.channel.stream.StreamChannel;

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

}
