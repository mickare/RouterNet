package de.rennschnitzel.net.core.channel.object;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.Serializable;

import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import com.google.protobuf.ByteString.Output;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.protocol.TransportProtocol;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public class ObjectConverters {

  private ObjectConverters() {
    // TODO Auto-generated constructor stub
  }

  public static TransportProtocol.TunnelRegister.Type getType(Class<?> dataClass) throws InvalidClassException {
    if (byte[].class.isAssignableFrom(dataClass)) {
      return TransportProtocol.TunnelRegister.Type.BYTES;
    }
    if (Void.class.equals(dataClass)) {
      return TransportProtocol.TunnelRegister.Type.OBJECT;
    }
    if (Serializable.class.isAssignableFrom(dataClass)) {
      return TransportProtocol.TunnelRegister.Type.OBJECT;
    }
    throw new InvalidClassException(dataClass.getName(), "data class not supported");
  }

  @SuppressWarnings("unchecked")
  public static <T> ObjectConverter<T> of(Class<T> dataClass) throws InvalidClassException {
    Preconditions.checkNotNull(dataClass);
    if (byte[].class.isAssignableFrom(dataClass)) {
      return (ObjectConverter<T>) BYTE_ARRAY;
    }
    if (Void.class.equals(dataClass)) {
      return (ObjectConverter<T>) VOID;
    }
    if (Serializable.class.isAssignableFrom(dataClass)) {
      return (ObjectConverter<T>) SerializableObjectConverter.of((Class<? extends Serializable>) dataClass);
    }
    throw new InvalidClassException(dataClass.getName(), "data class not supported");
  }

  @RequiredArgsConstructor
  private static abstract class AbstractObjectConverter<T> implements ObjectConverter<T> {
    @NonNull
    protected final Class<T> dataClass;

    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof AbstractObjectConverter)) {
        return false;
      }
      return this.dataClass.equals(((AbstractObjectConverter<?>) o).dataClass);
    }
  }

  private static class SerializableObjectConverter<T extends Serializable> extends AbstractObjectConverter<T> {

    public static <T extends Serializable> SerializableObjectConverter<T> of(Class<T> dataClass) {
      return new SerializableObjectConverter<T>(dataClass);
    }

    public SerializableObjectConverter(Class<T> dataClass) {
      super(dataClass);
    }

    @Override
    public final ByteString asByteString(final T obj) throws ConvertObjectChannelException {
      try (final Output stream = ByteString.newOutput()) {
        final FSTObjectOutput out = AbstractNetwork.SERIALIZATION.getObjectOutput(stream);
        out.writeObject(obj, this.dataClass);
        out.flush();
        return stream.toByteString();
      } catch (final IOException e) {
        throw new ConvertObjectChannelException(e);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public final T asObject(final ByteString byteData) throws ConvertObjectChannelException {
      try (final InputStream stream = byteData.newInput()) {
        final FSTObjectInput in = AbstractNetwork.SERIALIZATION.getObjectInput(stream);
        return (T) in.readObject(this.dataClass);
      } catch (final Exception e) {
        throw new ConvertObjectChannelException(e);
      }
    }
  }


  public static final ObjectConverter<byte[]> BYTE_ARRAY = new AbstractObjectConverter<byte[]>(byte[].class) {
    @Override
    public final ByteString asByteString(final byte[] obj) throws ConvertObjectChannelException {
      return ByteString.copyFrom(obj);
    }

    @Override
    public final byte[] asObject(final ByteString byteData) throws ConvertObjectChannelException {
      return byteData.toByteArray();
    }
  };

  public static final ObjectConverter<Void> VOID = new AbstractObjectConverter<Void>(Void.class) {
    @Override
    public final ByteString asByteString(final Void obj) throws ConvertObjectChannelException {
      return ByteString.copyFrom(new byte[0]);
    }

    @Override
    public final Void asObject(final ByteString byteData) throws ConvertObjectChannelException {
      if (byteData.size() > 0) {
        throw new ConvertObjectChannelException("unexpected byte data");
      }
      return null;
    }
  };

}
