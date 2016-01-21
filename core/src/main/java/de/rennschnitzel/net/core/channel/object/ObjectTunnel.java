package de.rennschnitzel.net.core.channel.object;

import java.io.IOException;
import java.io.InvalidClassException;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.logging.Level;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.Owner;
import de.rennschnitzel.net.core.Tunnel;
import de.rennschnitzel.net.core.Target;
import de.rennschnitzel.net.core.tunnel.AbstractSubTunnel;
import de.rennschnitzel.net.core.tunnel.AbstractSubTunnelDescriptor;
import de.rennschnitzel.net.core.tunnel.TunnelHandler;
import de.rennschnitzel.net.core.tunnel.TunnelMessage;
import de.rennschnitzel.net.core.tunnel.SubTunnel;
import de.rennschnitzel.net.core.tunnel.SubChannelDescriptor;
import de.rennschnitzel.net.protocol.TransportProtocol;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public class ObjectTunnel<T> extends AbstractSubTunnel<ObjectTunnel<T>, ObjectTunnel.Descriptor<T>>
    implements TunnelHandler, SubTunnel {

  public static class Descriptor<T> extends AbstractSubTunnelDescriptor<Descriptor<T>, ObjectTunnel<T>>
      implements SubChannelDescriptor<ObjectTunnel<T>> {

    @Getter
    private final Class<T> dataClass;
    @Getter
    private final ObjectConverter<T> converter;

    public Descriptor(String name, Class<T> dataClass) throws InvalidClassException {
      this(name, dataClass, ObjectConverters.of(dataClass));
    }

    public Descriptor(String name, Class<T> dataClass, ObjectConverter<T> converter) {
      super(name, TransportProtocol.TunnelRegister.Type.OBJECT);
      Preconditions.checkNotNull(dataClass);
      Preconditions.checkNotNull(converter);
      this.dataClass = dataClass;
      this.converter = converter;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Descriptor)) {
        return false;
      }
      Descriptor<?> d = (Descriptor<?>) o;
      return this.name.equals(d.name) && this.dataClass.equals(d.dataClass) && this.converter.equals(d.converter);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, dataClass, converter.getClass().getName());
    }

    @Override
    public ObjectTunnel<T> create(Tunnel parentChannel) {
      return new ObjectTunnel<>(parentChannel, this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObjectTunnel<T> cast(SubTunnel channel) {
      if (channel == null) {
        return null;
      }
      Preconditions.checkArgument(channel.getDescriptor() == this);
      return (ObjectTunnel<T>) channel;
    }

  }

  private final CopyOnWriteArraySet<RegisteredMessageListener> listeners = new CopyOnWriteArraySet<>();

  public ObjectTunnel(Tunnel parentChannel, Descriptor<T> descriptor) throws IllegalStateException {
    super(parentChannel, descriptor);
  }

  public final ObjectConverter<T> getConverter() {
    return this.descriptor.getConverter();
  }

  public void broadcast(T obj) throws ConvertObjectChannelException, IOException {
    this.send(Target.toAll(), obj);
  }

  public void send(Target target, T obj) throws ConvertObjectChannelException, IOException {
    send(new ObjectChannelMessage<T>(this, target, getNetwork().getHome().getId(), obj));
  }

  public void send(ObjectChannelMessage<T> ocmsg) throws IOException {
    this.parentTunnel.send(ocmsg);
  }

  @Override
  public void receive(TunnelMessage cmsg) throws ConvertObjectChannelException {
    this.receive(new ObjectChannelMessage<T>(this, cmsg));
  }

  public void receive(ObjectChannelMessage<T> ocmsg) {
    this.listeners.forEach(c -> c.accept(ocmsg));
  }

  public final void registerMessageListener(final Owner owner, final Consumer<ObjectChannelMessage<T>> dataConsumer) {
    listeners.add(new RegisteredMessageListener(owner, dataConsumer));
  }

  public final void unregisterListeners(final Owner owner) {
    this.listeners.removeIf((l) -> l.getOwner().equals(owner));
  }

  @Getter
  @RequiredArgsConstructor
  private class RegisteredMessageListener implements Consumer<ObjectChannelMessage<T>> {

    @NonNull
    private final Owner owner;
    @NonNull
    private final Consumer<ObjectChannelMessage<T>> delegate;

    @Override
    public void accept(ObjectChannelMessage<T> cmsg) {
      try {
        delegate.accept(cmsg);
      } catch (Exception e) {
        owner.getLogger().log(Level.SEVERE, "Message listener of " + owner.toString() + " threw exception: " + e.getMessage(), e);
      }
    }
  }

}