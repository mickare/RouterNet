package de.rennschnitzel.backbone.net.channel.object;

import java.io.IOException;
import java.io.InvalidClassException;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.logging.Level;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.Owner;
import de.rennschnitzel.backbone.net.NetworkMember;
import de.rennschnitzel.backbone.net.Target;
import de.rennschnitzel.backbone.net.channel.Channel;
import de.rennschnitzel.backbone.net.channel.ChannelHandler;
import de.rennschnitzel.backbone.net.channel.ChannelMessage;
import de.rennschnitzel.backbone.net.channel.SubChannel;
import de.rennschnitzel.backbone.net.channel.SubChannelDescriptor;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ChannelRegister.Type;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public class ObjectChannel<T> implements ChannelHandler<Channel, ChannelMessage>, SubChannel {

  public static class Descriptor<T> implements SubChannelDescriptor<ObjectChannel<T>> {
    @Getter
    private final String name;
    @Getter
    private final Class<T> dataClass;
    @Getter
    private final ObjectConverter<T> converter;

    @Getter
    private final TransportProtocol.ChannelRegister.Type type = TransportProtocol.ChannelRegister.Type.OBJECT;

    public Descriptor(String name, Class<T> dataClass) throws InvalidClassException {
      this(name, dataClass, ObjectConverters.of(dataClass));
    }

    public Descriptor(String name, Class<T> dataClass, ObjectConverter<T> converter) {
      Preconditions.checkArgument(!name.isEmpty());
      Preconditions.checkNotNull(dataClass);
      Preconditions.checkNotNull(converter);
      this.name = name.toLowerCase();
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
    public ObjectChannel<T> create(Owner owner, Channel parentChannel) {
      return new ObjectChannel<>(owner, parentChannel, this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObjectChannel<T> cast(SubChannel channel) {
      Preconditions.checkArgument(channel.getDescriptor() == this);
      return (ObjectChannel<T>) channel;
    }

  }

  @Getter
  private final Owner owner;
  @Getter
  private final Channel parentChannel;
  @Getter
  private final Descriptor<T> descriptor;

  private final CopyOnWriteArraySet<RegisteredMessageListener> listeners = new CopyOnWriteArraySet<>();

  public ObjectChannel(Owner owner, Channel parentChannel, Descriptor<T> descriptor) throws IllegalStateException {
    Preconditions.checkNotNull(owner);
    Preconditions.checkNotNull(parentChannel);
    Preconditions.checkNotNull(descriptor);
    this.owner = owner;
    this.parentChannel = parentChannel;
    this.descriptor = descriptor;
    this.parentChannel.registerHandler(this);
  }

  public boolean isClosed() {
    return this.parentChannel.isClosed();
  }

  public void close() {
    this.parentChannel.close();
  }

  public final ObjectConverter<T> getConverter() {
    return this.descriptor.getConverter();
  }

  public int getChannelId() {
    return this.parentChannel.getChannelId();
  }

  public String getName() {
    return this.parentChannel.getName();
  }

  public NetworkMember getHome() {
    return this.parentChannel.getHome();
  }

  public void send(Target target, T obj) throws ConvertObjectChannelException, IOException {
    send(new ObjectChannelMessage<T>(this, target, getHome().getId(), obj));
  }

  public void send(ObjectChannelMessage<T> ocmsg) throws IOException {
    this.parentChannel.send(ocmsg);
  }

  public void receive(ChannelMessage cmsg) throws ConvertObjectChannelException {
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

  @Override
  public Type getType() {
    return this.descriptor.getType();
  }

}
