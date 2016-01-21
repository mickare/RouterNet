package de.rennschnitzel.net.core;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import de.rennschnitzel.net.core.tunnel.SubTunnel;
import de.rennschnitzel.net.protocol.TransportProtocol;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import de.rennschnitzel.net.protocol.TransportProtocol.TunnelRegister;
import de.rennschnitzel.net.util.concurrent.CloseableLock;
import de.rennschnitzel.net.util.concurrent.CloseableReadWriteLock;
import de.rennschnitzel.net.util.concurrent.ReentrantCloseableReadWriteLock;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public abstract class Connection {

  private final AtomicInteger TUNNEL_ID_GENERATOR = new AtomicInteger(0);

  private final CloseableReadWriteLock tunnelLock = new ReentrantCloseableReadWriteLock();
  private final BiMap<Integer, String> tunnels = HashBiMap.create();
  private final LoadingCache<String, SettableFuture<Integer>> tunnelFutures = CacheBuilder.newBuilder()//
      .expireAfterWrite(1, TimeUnit.SECONDS)//
      .removalListener(new RemovalListener<String, SettableFuture<Integer>>() {
        @Override
        public void onRemoval(RemovalNotification<String, SettableFuture<Integer>> notification) {
          notification.getValue().cancel(true);
        }
      })//
      .build(CacheLoader.from(SettableFuture::create));

  @Getter
  @NonNull
  private final AbstractNetwork network;

  @Getter
  @Setter(AccessLevel.PROTECTED)
  @NonNull
  private UUID id = null;

  @Getter
  @Setter
  @NonNull
  private CloseMessage closeMessage = null;

  public Connection(AbstractNetwork network, UUID id) {
    this(network);
    Preconditions.checkNotNull(id);
    this.id = id;
  }

  private int getNextFreeTunnelId() {
    int id;
    try (CloseableLock l = tunnelLock.readLock().open()) {
      do {
        id = TUNNEL_ID_GENERATOR.incrementAndGet();
      } while (tunnels.containsKey(id));
    }
    return id;
  }

  private int getNextFreeTunnelId(int proposedId) {
    int id = proposedId;
    try (CloseableLock l = tunnelLock.readLock().open()) {
      while (tunnels.containsKey(id)) {
        id = TUNNEL_ID_GENERATOR.incrementAndGet();
      }
    }
    return id;
  }

  public String getTunnelNameIfPresent(int id) {
    try (CloseableLock l = tunnelLock.readLock().open()) {
      return this.tunnels.get(id);
    }
  }

  public Integer getTunnelIdIfPresent(String name) {
    try (CloseableLock l = tunnelLock.readLock().open()) {
      return this.tunnels.inverse().get(name);
    }
  }

  public Integer getTunnelIdIfPresent(Tunnel channel) {
    return getTunnelIdIfPresent(channel.getName());
  }

  public Integer getTunnelIdIfPresent(SubTunnel channel) {
    return getTunnelIdIfPresent(channel.getName());
  }

  public int getTunnelId(Tunnel channel) throws IOException {
    Integer id = getTunnelIdIfPresent(channel);
    if (id == null) {
      try {
        id = registerTunnel(channel).get(1, TimeUnit.SECONDS);
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        throw new IOException(e);
      }
    }
    return id;
  }

  public int getTunnelId(SubTunnel channel) throws IOException {
    return getTunnelId(channel.getParentTunnel());
  }

  public abstract void send(TransportProtocol.Packet packet) throws IOException;

  public void send(TransportProtocol.Packet.Builder packet) throws IOException {
    send(packet.build());
  }

  public void sendTunnelMessage(TransportProtocol.TunnelMessage msg) throws IOException {
    send(TransportProtocol.Packet.newBuilder().setTunnelMessage(msg).build());
  }

  public void sendTunnelMessage(TransportProtocol.TunnelMessage.Builder msg) throws IOException {
    sendTunnelMessage(msg.build());
  }

  public abstract boolean isClosed();

  public abstract boolean isActive();

  protected ListenableFuture<Integer> registerTunnel(Tunnel tunnel) throws IOException {

    SettableFuture<Integer> future = this.tunnelFutures.getUnchecked(tunnel.getName());

    try (CloseableLock l = tunnelLock.writeLock().open()) {
      if (future.isDone()) {
        try {
          this.tunnels.put(future.get(1, TimeUnit.SECONDS), tunnel.getName());
          return future;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
          future = SettableFuture.create();
          this.tunnelFutures.put(tunnel.getName(), future);
        }
      }
    }

    TransportProtocol.TunnelRegister.Builder b = TransportProtocol.TunnelRegister.newBuilder();
    b.setTunnelId(this.getNextFreeTunnelId());
    b.setName(tunnel.getName());
    b.setType(tunnel.getType());
    b.setRequest(true);
    send(Packet.newBuilder().setTunnelRegister(b));

    return future;

  }

  public ListenableFuture<Integer> registerTunnel(TunnelRegister msg) throws IOException {


    SettableFuture<Integer> future = this.tunnelFutures.getUnchecked(msg.getName());


    try (CloseableLock l = tunnelLock.writeLock().open()) {

      if (msg.getRequest()) {

        // Is local future already done?
        if (future.isDone()) {
          try {

            int id = future.get(1, TimeUnit.SECONDS);

            this.tunnels.put(id, msg.getName());
            send(Packet.newBuilder().setTunnelRegister(//
                TunnelRegister.newBuilder(msg).setTunnelId(id).setRequest(false))//
            );

            return future;

          } catch (InterruptedException | ExecutionException | TimeoutException e) {
            // Failed, so then retry!
            future = SettableFuture.create();
            this.tunnelFutures.put(msg.getName(), future);
          }
        }
        // There was no future, it could be already forgotten (a new future had been created) or it
        // failed.

        // Is there already a channel registered?
        Integer existingId = this.tunnels.inverse().get(msg.getName());

        if (existingId != null) {

          if (future.set(existingId.intValue())) {
            send(Packet.newBuilder().setTunnelRegister(//
                TunnelRegister.newBuilder(msg).setTunnelId(existingId.intValue()).setRequest(false))//
            );
          }

        } else {

          int id = getNextFreeTunnelId(msg.getTunnelId());
          if (future.set(id)) {
            this.tunnels.put(id, msg.getName());
            send(Packet.newBuilder().setTunnelRegister(//
                TunnelRegister.newBuilder(msg).setTunnelId(id).setRequest(false))//
            );
          }

        }

      } else {

        // Register without any question!

        this.tunnels.put(msg.getTunnelId(), msg.getName());
        future.set(msg.getTunnelId());

      }

    }

    return future;

  }


}
