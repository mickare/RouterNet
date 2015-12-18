package de.rennschnitzel.backbone.net.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.logging.Level;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;

import de.rennschnitzel.backbone.Owner;
import de.rennschnitzel.backbone.net.Connection;
import de.rennschnitzel.backbone.net.NetworkMember;
import de.rennschnitzel.backbone.net.Target;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.Packet;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public class Channel {

  private final Connection connection;
  @Getter
  private boolean closed;
  @Getter
  private final int channelId;
  @Getter
  private final String name;

  private ChannelHandler handler = null;

  private final CopyOnWriteArraySet<RegisteredMessageListener> listeners = new CopyOnWriteArraySet<>();


  public Channel(Connection connection, int channelId, String name) {
    Preconditions.checkNotNull(connection);
    Preconditions.checkArgument(!name.isEmpty());
    this.connection = connection;
    this.closed = connection.isClosed();
    this.channelId = channelId;
    this.name = name.toLowerCase();
  }

  public synchronized void registerHandler(ChannelHandler handler) throws IllegalStateException {
    Preconditions.checkNotNull(handler);
    Preconditions.checkState(this.handler == null);
    this.handler = handler;
  }

  public TransportProtocol.ChannelRegister.Type getType() {
    if (handler != null) {
      return handler.getType();
    }
    return TransportProtocol.ChannelRegister.Type.BYTES;
  }

  public void close() {
    this.closed = true;
  }

  public NetworkMember getHome() {
    return this.connection.getHome();
  }

  public void register() {
    if (isClosed()) {
      return;
    }
    TransportProtocol.ChannelRegister.Builder b = TransportProtocol.ChannelRegister.newBuilder();
    b.setChannelId(this.channelId);
    b.setName(this.name);
    b.setType(getType());
    this.connection.send(Packet.newBuilder().setChannelRegister(b));

  }

  public void send(Target target, ByteBuffer data) throws IOException {
    this.send(target, ByteString.copyFrom(data));
  }

  public void send(Target target, byte[] data) throws IOException {
    this.send(target, ByteString.copyFrom(data));
  }

  public void send(Target target, ByteString data) throws IOException {
    final ChannelMessage cmsg = new ChannelMessage(this, target, this.getHome().getId(), data);
    this.send(cmsg);
  }

  public void send(ChannelMessage cmsg) throws IOException {
    this.sendIgnoreSelf(cmsg);
    if (cmsg.getTarget().contains(getHome())) {
      this.receive(cmsg);
    }
  }

  public void sendIgnoreSelf(ChannelMessage cmsg) throws IOException {
    this.connection.sendChannelMessage(cmsg.getProtocolMessage());
  }

  public final void receive(final TransportProtocol.ChannelMessage msg) {
    this.receive(new ChannelMessage(this, msg));
  }

  public final void receive(final ChannelMessage cmsg) {
    this.listeners.forEach(c -> c.accept(cmsg));
    if (this.handler != null) {
      try {
        this.handler.receive(cmsg);
      } catch (Exception e) {
        this.handler.getOwner().getLogger().log(Level.SEVERE, "Channel handler exception: " + e.getMessage(), e);
      }
    }
  }

  public final void registerMessageListener(final Owner owner, final Consumer<ChannelMessage> dataConsumer) {
    listeners.add(new RegisteredMessageListener(owner, dataConsumer));
  }

  @Getter
  @RequiredArgsConstructor
  private static class RegisteredMessageListener implements Consumer<ChannelMessage> {

    @NonNull
    private final Owner owner;
    @NonNull
    private final Consumer<ChannelMessage> delegate;

    @Override
    public void accept(ChannelMessage cmsg) {
      try {
        delegate.accept(cmsg);
      } catch (Exception e) {
        owner.getLogger().log(Level.SEVERE, "Message listener of " + owner.toString() + " threw exception: " + e.getMessage(), e);
      }
    }
  }


}
