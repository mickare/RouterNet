package de.rennschnitzel.net.core;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.logging.Level;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;

import de.rennschnitzel.net.Owner;
import de.rennschnitzel.net.core.Node.HomeNode;
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

  @Getter
  private final int id;

  private Optional<TunnelRegister.Type> type = Optional.empty();
  private TunnelHandler handler = null;

  private final CopyOnWriteArraySet<RegisteredMessageListener> listeners = new CopyOnWriteArraySet<>();

  @Getter
  private volatile boolean closed = false;

  public Tunnel(final AbstractNetwork network, final String name) {
    Preconditions.checkNotNull(network);
    Preconditions.checkArgument(!name.isEmpty());
    this.network = network;
    this.name = name.toLowerCase();
    this.id = this.name.hashCode();
  }

  public synchronized void registerHandler(final TunnelHandler handler) throws IllegalStateException {
    Preconditions.checkNotNull(handler);
    Preconditions.checkState(this.handler == null);
    if (this.type.isPresent()) {
      Preconditions.checkState(handler.getType() == this.type.get());
    }
    this.handler = handler;
    this.type = Optional.of(handler.getType());
  }

  public synchronized void setType(final TunnelRegister.Type type) {
    Preconditions.checkNotNull(type);
    if (this.type.isPresent()) {
      if (this.type.get() == type) {
        return;
      }
      throw new IllegalStateException("Type already defined as " + this.type.get() + "!");
    }
    this.type = Optional.of(type);
  }

  public TunnelRegister.Type getType() {
    return type.orElse(TunnelRegister.Type.BYTES);
  }

  public void close() {
    this.closed = true;
  }

  public void broadcast(final ByteBuffer data) {
    this.send(Target.toAll(), data);
  }

  public void broadcast(final byte[] data) {
    this.send(Target.toAll(), data);
  }

  public void broadcast(final ByteString data) {
    this.send(Target.toAll(), data);
  }

  public void send(final Target target, final ByteBuffer data) {
    this.send(target, ByteString.copyFrom(data));
  }

  public void send(final Target target, final byte[] data) {
    this.send(target, ByteString.copyFrom(data));
  }

  public void send(final Target target, final ByteString data) {
    final TunnelMessage cmsg = new TunnelMessage(this, target, getNetwork().getHome().getId(), data);
    this.send(cmsg);
  }

  public void send(final TunnelMessage cmsg) {
    final HomeNode home = getNetwork().getHome();
    if (!cmsg.getTarget().isOnly(home)) {
      this.sendIgnoreSelf(cmsg);
    }
    if (cmsg.getTarget().contains(getNetwork().getHome())) {
      this.receive(cmsg);
    }
  }

  private void sendIgnoreSelf(final TunnelMessage cmsg) {
    this.network.sendTunnelMessage(cmsg);
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

  public void register(Connection connection) {
    TunnelRegister.Builder b = TunnelRegister.newBuilder();
    b.setTunnelId(this.getId());
    b.setName(this.getName());
    b.setType(this.getType());
    connection.writeAndFlushFast(b.build());
  }

  @Getter
  @RequiredArgsConstructor
  private class RegisteredMessageListener implements Consumer<TunnelMessage> {

    @NonNull
    private final Owner owner;
    @NonNull
    private final Consumer<TunnelMessage> delegate;

    @Override
    public void accept(TunnelMessage cmsg) {
      try {
        delegate.accept(cmsg);
      } catch (Exception e) {
        getNetwork().getLogger().log(Level.SEVERE, "Message listener of " + owner + " threw exception: " + e.getMessage(), e);
      }
    }
  }


}
