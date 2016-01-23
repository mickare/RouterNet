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
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import de.rennschnitzel.net.core.tunnel.SubTunnel;
import de.rennschnitzel.net.exception.ConnectionException;
import de.rennschnitzel.net.protocol.TransportProtocol;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ErrorMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import de.rennschnitzel.net.protocol.TransportProtocol.TunnelRegister;
import de.rennschnitzel.net.util.FutureUtils;
import de.rennschnitzel.net.util.concurrent.CloseableLock;
import de.rennschnitzel.net.util.concurrent.CloseableReadWriteLock;
import de.rennschnitzel.net.util.concurrent.ReentrantCloseableReadWriteLock;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public abstract class Connection implements PacketOutWriter {

  private final AtomicInteger TUNNEL_ID_GENERATOR = new AtomicInteger(0);

  private final CloseableReadWriteLock tunnelLock = new ReentrantCloseableReadWriteLock();
  private final BiMap<Integer, String> tunnels = HashBiMap.create();
  private final LoadingCache<String, Promise<Integer>> tunnelFutures = CacheBuilder.newBuilder()//
      .expireAfterWrite(1, TimeUnit.SECONDS)//
      .removalListener(new RemovalListener<String, Promise<Integer>>() {
        @Override
        public void onRemoval(RemovalNotification<String, Promise<Integer>> notification) {
          if (notification.getCause() == RemovalCause.EXPIRED) {
            notification.getValue().tryFailure(new TimeoutException("took longer than one second"));
          } else {
            notification.getValue().tryFailure(new Exception("was removed because " + notification.getCause()));
          }
        }
      })//
      .build(CacheLoader.from(ImmediateEventExecutor.INSTANCE::newPromise));

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


  public abstract boolean isOpen();

  public abstract boolean isActive();

  public abstract Future<?> disconnect(CloseMessage msg);


  public Future<?> disconnect(CloseMessage.Builder builder) {
    return this.disconnect(builder.build());
  }

  public Future<?> disconnect(String reason) {
    return this.disconnect(CloseMessage.newBuilder().setNormal(reason));
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
      return this.tunnels.inverse().get(name.toLowerCase());
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
        id = _registerTunnel(channel).get(1, TimeUnit.SECONDS);
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        throw new IOException(e);
      }
    }
    return id;
  }

  public int getTunnelId(SubTunnel channel) throws IOException {
    return getTunnelId(channel.getParentTunnel());
  }

  public Future<?> sendTunnelMessage(TransportProtocol.TunnelMessage msg) {
    return send(TransportProtocol.Packet.newBuilder().setTunnelMessage(msg).build());
  }

  public Future<?> sendTunnelMessage(TransportProtocol.TunnelMessage.Builder msg) {
    return sendTunnelMessage(msg.build());
  }


  public Future<Integer> registerTunnel(Tunnel tunnel) {
    Integer id = this.getTunnelIdIfPresent(tunnel);
    if (id != null) {
      return FutureUtils.futureSuccess(id);
    }
    return _registerTunnel(tunnel);
  }

  private Future<Integer> _registerTunnel(Tunnel tunnel) {

    Promise<Integer> future = this.tunnelFutures.getUnchecked(tunnel.getName());

    try (CloseableLock l = tunnelLock.writeLock().open()) {
      if (future.isDone()) {
        try {
          this.tunnels.put(future.get(), tunnel.getName());
          return future;
        } catch (InterruptedException | ExecutionException e) {
          this.tunnelFutures.refresh(tunnel.getName());
          future = this.tunnelFutures.getUnchecked(tunnel.getName());
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

  public Future<Integer> registerTunnel(TunnelRegister msg) throws IOException {


    Promise<Integer> future = this.tunnelFutures.getUnchecked(msg.getName());


    try (CloseableLock l = tunnelLock.writeLock().open()) {

      if (msg.getRequest()) {

        // Is local future already done?
        if (future.isDone()) {
          try {

            int id = future.get(1, TimeUnit.SECONDS);

            this.tunnels.put(id, msg.getName());
            send(Packet.newBuilder().setTunnelRegister(//
                TunnelRegister.newBuilder(msg).setTunnelId(id).setRequest(false))//
            ).get(100, TimeUnit.MILLISECONDS);

            return future;

          } catch (InterruptedException | ExecutionException | TimeoutException e) {
            // Failed, so then retry!
            this.tunnelFutures.refresh(msg.getName());
            future = this.tunnelFutures.getUnchecked(msg.getName());
          }
        }
        // There was no future, it could be already forgotten (a new future had been created) or it
        // failed.

        // Is there already a channel registered?
        Integer existingId = this.tunnels.inverse().get(msg.getName());

        if (existingId != null) {

          if (future.trySuccess(existingId.intValue())) {
            send(Packet.newBuilder().setTunnelRegister(//
                TunnelRegister.newBuilder(msg).setTunnelId(existingId.intValue()).setRequest(false))//
            ).get(100, TimeUnit.MILLISECONDS);
          }

        } else {

          int id = getNextFreeTunnelId(msg.getTunnelId());
          if (future.trySuccess(id)) {
            this.tunnels.put(id, msg.getName());
            send(Packet.newBuilder().setTunnelRegister(//
                TunnelRegister.newBuilder(msg).setTunnelId(id).setRequest(false))//
            ).get(100, TimeUnit.MILLISECONDS);
          }

        }

      } else {

        // Register without any question!

        this.tunnels.put(msg.getTunnelId(), msg.getName());
        future.trySuccess(msg.getTunnelId());

      }

    } catch (Exception e) {
      future.tryFailure(e);
      throw new ConnectionException(ErrorMessage.Type.UNAVAILABLE, "failed to register tunnel", e);
    }

    return future;

  }


}
