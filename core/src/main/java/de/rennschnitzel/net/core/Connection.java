package de.rennschnitzel.net.core;

import java.util.UUID;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

import de.rennschnitzel.net.core.packet.PacketWriter;
import de.rennschnitzel.net.exception.ConnectionException;
import de.rennschnitzel.net.netty.ChannelWrapper;
import de.rennschnitzel.net.protocol.TransportProtocol;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ErrorMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.Future;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public @Getter class Connection implements PacketWriter<ChannelFuture> {


  private final AbstractNetwork network;
  private final UUID peerId;
  private final ChannelWrapper channel;
  private @Setter @NonNull CloseMessage closeMessage = null;

  private final BiMap<Integer, String> remoteTunnels = Maps.synchronizedBiMap(HashBiMap.create());
  private final BiMap<Integer, String> remoteTunnelsUnmodif = Maps.unmodifiableBiMap(this.remoteTunnels);

  public Connection(AbstractNetwork network, UUID peerId, ChannelWrapper channel) {
    Preconditions.checkNotNull(network);
    Preconditions.checkNotNull(peerId);
    Preconditions.checkNotNull(channel);
    this.network = network;
    this.peerId = peerId;
    this.channel = channel;
  }

  public String getName() {
    return network.getNodeUnsafe(peerId).getName().orElse(null);
  }

  public boolean isOpen() {
    return channel.isOpen();
  }

  public boolean isActive() {
    return channel.isActive();
  }

  public Future<?> disconnect(CloseMessage msg) {
    if (channel.isOpen()) {
      this.closeMessage = msg;
    }
    return channel.writeAndFlush(msg).addListener(ChannelFutureListener.CLOSE);
  }

  public Future<?> disconnect(CloseMessage.Builder builder) {
    return this.disconnect(builder.build());
  }

  public Future<?> disconnect(String reason) {
    return this.disconnect(CloseMessage.newBuilder().setNormal(reason));
  }

  public Future<?> disconnect(ErrorMessage error) {
    return this.disconnect(CloseMessage.newBuilder().setError(error));
  }

  public Future<?> disconnect(ErrorMessage.Builder builder) {
    return this.disconnect(builder.build());
  }


  @Override
  public void flush() {
    channel.flush();
  }


  @Override
  public void writeFast(Packet packet) {
    channel.writeFast(packet);
  }

  @Override
  public ChannelFuture write(Packet packet) {
    return channel.write(packet);
  }


  @Override
  public void writeAndFlushFast(Packet packet) {
    channel.writeAndFlushFast(packet);
  }

  @Override
  public ChannelFuture writeAndFlush(Packet packet) {
    return channel.writeAndFlush(packet);
  }

  @Override
  public String toString() {
    return "Connection[" + this.peerId + "]";
  }

  public void receive(TransportProtocol.TunnelRegister msg) throws ConnectionException {
    this.network.receiveTunnelRegister(msg);
    this.registerRemoteTunnel(msg.getTunnelId(), msg.getName());
  }

  public void registerRemoteTunnel(int id, String name) {
    remoteTunnels.forcePut(id, name.toLowerCase());
  }

  public BiMap<Integer, String> getRemoteTunnels() {
    return remoteTunnelsUnmodif;
  }

  public void addToNetwork() {
    this.network.addConnection(this);
  }

  public void removeFromNetwork() {
    this.network.removeConnection(this);
  }

}
