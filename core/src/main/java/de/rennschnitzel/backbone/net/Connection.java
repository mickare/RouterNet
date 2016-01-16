package de.rennschnitzel.backbone.net;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.Owner;
import de.rennschnitzel.backbone.net.channel.Channel;
import de.rennschnitzel.backbone.net.channel.SubChannel;
import de.rennschnitzel.backbone.net.channel.SubChannelDescriptor;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ChannelRegister;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.CloseMessage;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class Connection {

  private final AtomicInteger CHANNEL_ID_GENERATOR = new AtomicInteger(0);

  private final ConcurrentMap<String, Channel> channelsByName = new ConcurrentHashMap<>();
  private final ConcurrentMap<Integer, Channel> channelsById = new ConcurrentHashMap<>();

  private final ConcurrentMap<SubChannelDescriptor<?>, SubChannel> subChannels = new ConcurrentHashMap<>();

  @Getter
  @NonNull
  private final Network network;

  public Channel getChannelIfPresent(String name) {
    return this.channelsByName.get(name.toLowerCase());
  }

  public Channel getChannelIfPresent(int channelId) {
    return this.channelsById.get(channelId);
  }

  private synchronized int getNextFreeChannelId() {
    int id;
    do {
      id = CHANNEL_ID_GENERATOR.incrementAndGet();
    } while (channelsById.containsKey(id));
    return id;
  }

  public Channel getChannel(String name) {
    return getChannel(name, true);
  }

  public Channel getChannel(String name, boolean register) {
    final String key = name.toLowerCase();
    Channel channel = this.channelsByName.get(key);
    if (channel == null) {
      synchronized (this) {
        // Check again, but in synchronized state!
        channel = this.channelsByName.get(key);
        if (channel == null) {
          channel = new Channel(this, getNextFreeChannelId(), key);
          this.channelsByName.put(channel.getName(), channel);
          this.channelsById.put(channel.getChannelId(), channel);
          if (register) {
            channel.register();
          }
        }
      }
    }
    return channel;
  }

  public <S extends SubChannel> S getChannelIfPresent(SubChannelDescriptor<S> descriptor) {
    Preconditions.checkNotNull(descriptor);
    return descriptor.cast(this.subChannels.get(descriptor));
  }

  public <S extends SubChannel> S getChannel(SubChannelDescriptor<S> descriptor, Owner owner) {
    Preconditions.checkNotNull(descriptor);
    Preconditions.checkNotNull(owner);
    S subChannel = getChannelIfPresent(descriptor);
    if (subChannel == null) {
      synchronized (this) {
        // Check again, but in synchronized state!
        subChannel = getChannelIfPresent(descriptor);
        if (subChannel == null) {
          Channel channel = getChannel(descriptor.getName(), false);
          subChannel = descriptor.create(owner, channel);
          this.subChannels.put(descriptor, subChannel);
          channel.register();
        }
      }
    }
    return subChannel;
  }

  public abstract void send(TransportProtocol.Packet packet);

  public void send(TransportProtocol.Packet.Builder packet) {
    send(packet.build());
  }

  public void sendChannelMessage(TransportProtocol.ChannelMessage msg) throws IOException {
    send(TransportProtocol.Packet.newBuilder().setChannelMessage(msg).build());
  }

  public void sendChannelMessage(TransportProtocol.ChannelMessage.Builder msg) {
    send(TransportProtocol.Packet.newBuilder().setChannelMessage(msg).build());
  }

  public abstract boolean isClosed();

  public abstract boolean remoteClose(CloseMessage msg);

  private static boolean isDifferentChannelRegister(Channel dupl, ChannelRegister msg) {
    if (dupl != null) {
      if (dupl.getChannelId() != msg.getChannelId() || dupl.getType() != msg.getType() || !dupl.getName().equalsIgnoreCase(msg.getName())) {
        return true;
      }
    }
    return false;
  }

  public void registerChannel(ChannelRegister msg) {

    synchronized (this) {

      final String key = msg.getName().toLowerCase();
      Channel old = this.channelsByName.get(key);
      if (isDifferentChannelRegister(old, msg)) {
        old.close();
        this.channelsByName.remove(key, old);
      }

      // There could be one with a different id... find it!
      Channel old2 = this.channelsById.get(msg.getChannelId());
      if (isDifferentChannelRegister(old2, msg)) {
        old2.close();
        this.channelsById.remove(msg.getChannelId(), old2);
      }

      Channel channel = new Channel(this, msg.getChannelId(), key);
      channel.setType(msg.getType());
      this.channelsByName.put(channel.getName(), channel);
      this.channelsById.put(channel.getChannelId(), channel);

    }
  }


}
