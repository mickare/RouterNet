package de.rennschnitzel.backbone.net;

import java.io.IOException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import de.rennschnitzel.backbone.Owner;
import de.rennschnitzel.backbone.net.channel.Channel;
import de.rennschnitzel.backbone.net.channel.SubChannel;
import de.rennschnitzel.backbone.net.channel.SubChannelDescriptor;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;

public abstract class Connection {

  private final AtomicInteger ID_GENERATOR = new AtomicInteger(0);

  private final ConcurrentMap<String, Channel> channelsByName = Maps.newConcurrentMap();
  private final ConcurrentMap<Integer, Channel> channelsById = Maps.newConcurrentMap();

  private final ConcurrentMap<SubChannelDescriptor<?>, SubChannel> subChannels = Maps.newConcurrentMap();

  public Channel getChannel(String name) {
    return this.channelsByName.get(name.toLowerCase());
  }

  public Channel getChannel(int channelId) {
    return this.channelsById.get(channelId);
  }

  private synchronized int getNextFreeInteger() {
    int id;
    do {
      id = ID_GENERATOR.incrementAndGet();
    } while (channelsById.containsKey(id));
    return id;
  }

  public Channel getOrCreateChannel(String name) {
    return getOrCreateChannel(name, true);
  }

  public Channel getOrCreateChannel(String name, boolean register) {
    final String key = name.toLowerCase();
    Channel channel = this.channelsByName.get(key);
    if (channel == null) {
      synchronized (this) {
        // Check again, but in synchronized state!
        channel = this.channelsByName.get(key);
        if (channel == null) {
          channel = new Channel(this, getNextFreeInteger(), key);
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

  public <S extends SubChannel> S getSubChannel(SubChannelDescriptor<S> descriptor) {
    Preconditions.checkNotNull(descriptor);
    return descriptor.cast(this.subChannels.get(descriptor));
  }

  public <S extends SubChannel> S getOrCreateSubChannel(SubChannelDescriptor<S> descriptor, Owner owner) {
    Preconditions.checkNotNull(descriptor);
    Preconditions.checkNotNull(owner);
    S subChannel = getSubChannel(descriptor);
    if (subChannel == null) {
      synchronized (this) {
        // Check again, but in synchronized state!
        subChannel = getSubChannel(descriptor);
        if (subChannel == null) {
          Channel channel = getOrCreateChannel(descriptor.getName(), false);
          subChannel = descriptor.create(owner, channel);
          this.subChannels.put(descriptor, subChannel);
          channel.register();
        }
      }
    }
    return subChannel;
  }

  public abstract NetworkMember getHome();

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



}
