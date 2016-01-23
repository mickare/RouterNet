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
import de.rennschnitzel.net.util.FutureUtils;
import io.netty.util.concurrent.Future;
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

  public Future<?> broadcast(ByteBuffer data) {
    return this.send(Target.toAll(), data);
  }

  public Future<?> broadcast(byte[] data) {
    return this.send(Target.toAll(), data);
  }

  public Future<?> broadcast(ByteString data) {
    return this.send(Target.toAll(), data);
  }

  public Future<?> send(Target target, ByteBuffer data) {
    return this.send(target, ByteString.copyFrom(data));
  }

  public Future<?> send(Target target, byte[] data) {
    return this.send(target, ByteString.copyFrom(data));
  }

  public Future<?> send(Target target, ByteString data) {
    final TunnelMessage cmsg = new TunnelMessage(this, target, getNetwork().getHome().getId(), data);
    return this.send(cmsg);
  }

  public Future<?> send(TunnelMessage cmsg) {
    HomeNode home = getNetwork().getHome();
    Future<?> result = FutureUtils.SUCCESS;
    if (!cmsg.getTarget().isOnly(home)) {
      result = this.sendIgnoreSelf(cmsg);
    }
    if (cmsg.getTarget().contains(getNetwork().getHome())) {
      this.receive(cmsg);
    }
    return result;
  }

  public Future<?> sendIgnoreSelf(TunnelMessage cmsg) {
    return this.network.sendTunnelMessage(cmsg);
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

  public Future<Integer> register() {
    return this.network.registerTunnel(this);
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
