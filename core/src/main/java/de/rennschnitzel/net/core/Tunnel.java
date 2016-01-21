package de.rennschnitzel.net.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.logging.Level;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;

import de.rennschnitzel.net.Owner;
import de.rennschnitzel.net.core.tunnel.TunnelHandler;
import de.rennschnitzel.net.core.tunnel.TunnelMessage;
import de.rennschnitzel.net.protocol.TransportProtocol;
import de.rennschnitzel.net.protocol.TransportProtocol.TunnelRegister;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public class Tunnel {

  @Getter
  private final AbstractNetwork network;

  @Getter
  private final String name;

  private Optional<TunnelRegister.Type> type = Optional.empty();
  private TunnelHandler handler = null;

  private final CopyOnWriteArraySet<RegisteredMessageListener> listeners = new CopyOnWriteArraySet<>();

  @Getter
  private volatile boolean closed = false;

  public Tunnel(AbstractNetwork network, String name) {
    Preconditions.checkNotNull(network);
    Preconditions.checkArgument(!name.isEmpty());
    this.network = network;
    this.name = name.toLowerCase();
  }

  public synchronized void registerHandler(TunnelHandler handler) throws IllegalStateException {
    Preconditions.checkNotNull(handler);
    Preconditions.checkState(this.handler == null);
    if (this.type.isPresent()) {
      Preconditions.checkState(handler.getType() == this.type.get());
    }
    this.handler = handler;
    this.type = Optional.of(handler.getType());
  }

  public synchronized void setType(TunnelRegister.Type type) {
    Preconditions.checkNotNull(type);
    Preconditions.checkState(!this.type.isPresent());
    this.type = Optional.of(type);
  }

  public TunnelRegister.Type getType() {
    return type.orElse(TunnelRegister.Type.BYTES);
  }

  public void close() {
    this.closed = true;
  }

  public void broadcast(ByteBuffer data) throws IOException {
    this.send(Target.toAll(), data);
  }

  public void broadcast(byte[] data) throws IOException {
    this.send(Target.toAll(), data);
  }

  public void broadcast(ByteString data) throws IOException {
    this.send(Target.toAll(), data);
  }

  public void send(Target target, ByteBuffer data) throws IOException {
    this.send(target, ByteString.copyFrom(data));
  }

  public void send(Target target, byte[] data) throws IOException {
    this.send(target, ByteString.copyFrom(data));
  }

  public void send(Target target, ByteString data) throws IOException {
    final TunnelMessage cmsg = new TunnelMessage(this, target, getNetwork().getHome().getId(), data);
    this.send(cmsg);
  }

  public void send(TunnelMessage cmsg) throws IOException {
    this.sendIgnoreSelf(cmsg);
    if (cmsg.getTarget().contains(getNetwork().getHome())) {
      this.receive(cmsg);
    }
  }

  public void sendIgnoreSelf(TunnelMessage cmsg) throws IOException {
    this.network.sendChannelMessage(cmsg);
  }

  public final void receiveProto(final TransportProtocol.TunnelMessage msg) {
    this.receive(new TunnelMessage(this, msg));
  }

  public final void receive(final TunnelMessage cmsg) {
    this.listeners.forEach(c -> c.accept(cmsg));
    if (this.handler != null) {
      try {
        this.handler.receive(cmsg);
      } catch (Exception e) {
        this.getNetwork().getLogger().log(Level.SEVERE, "Channel handler exception: " + e.getMessage(), e);
      }
    }
  }

  public final void registerMessageListener(final Owner owner, final Consumer<TunnelMessage> dataConsumer) {
    listeners.add(new RegisteredMessageListener(owner, dataConsumer));
  }

  @Getter
  @RequiredArgsConstructor
  private static class RegisteredMessageListener implements Consumer<TunnelMessage> {

    @NonNull
    private final Owner owner;
    @NonNull
    private final Consumer<TunnelMessage> delegate;

    @Override
    public void accept(TunnelMessage cmsg) {
      try {
        delegate.accept(cmsg);
      } catch (Exception e) {
        owner.getLogger().log(Level.SEVERE, "Message listener of " + owner.toString() + " threw exception: " + e.getMessage(), e);
      }
    }
  }


}
