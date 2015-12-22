package de.rennschnitzel.backbone.net;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import org.nustaq.serialization.FSTConfiguration;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;

import de.rennschnitzel.backbone.net.channel.ChannelDescriptors;
import de.rennschnitzel.backbone.net.channel.object.ObjectChannel;
import de.rennschnitzel.backbone.net.channel.stream.StreamChannel;
import de.rennschnitzel.backbone.net.node.HomeNode;
import de.rennschnitzel.backbone.net.node.NetworkNode;
import de.rennschnitzel.backbone.net.procedure.ProcedureCall;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureResponseMessage;
import de.rennschnitzel.backbone.net.store.DataStore;
import lombok.Getter;

public abstract class Network {

  public static FSTConfiguration SERIALIZATION = FSTConfiguration.createDefaultConfiguration();

  @Getter
  private static Network instance = null;

  // ******************************************************************************************
  // static start

  private static final ConcurrentMap<String, ChannelDescriptors> descriptors = new MapMaker().weakValues().makeMap();

  public static ObjectChannel.Descriptor<byte[]> getByteChannel(String name) {
    descriptors.get(name.toLowerCase());
    return ChannelDescriptors.getByteChannel(name);
  }

  public static <T extends Serializable> ObjectChannel.Descriptor<T> getObjectChannel(String name, Class<T> dataClass) {
    return ChannelDescriptors.getObjectChannel(name, dataClass);
  }

  public static StreamChannel.Descriptor getStreamChannelOut(String name) {
    return ChannelDescriptors.getStreamChannelOut(name);
  }

  public static StreamChannel.Descriptor getStreamChannelOut(String name, int bufferSize) {
    return ChannelDescriptors.getStreamChannelOut(name, bufferSize);
  }


  public static StreamChannel.Descriptor getStreamChannelIn(String name) {
    return ChannelDescriptors.getStreamChannelIn(name);
  }

  public static StreamChannel.Descriptor getStreamChannelIn(String name, int bufferSize) {
    return ChannelDescriptors.getStreamChannelIn(name, bufferSize);
  }


  // static end
  // ******************************************************************************************


  private final ConcurrentMap<UUID, NetworkNode> servers = new ConcurrentHashMap<>();

  @Getter
  private final EventBus eventBus = new EventBus();


  public abstract HomeNode getHome();

  public abstract Logger getLogger();

  protected void setInstance() {
    Network.instance = this;
  }

  public Map<UUID, NetworkNode> getServers() {
    return Collections.unmodifiableMap(servers);
  }

  public Map<UUID, NetworkNode> getServersOfTarget(Target target) {
    ImmutableMap.Builder<UUID, NetworkNode> b = ImmutableMap.builder();
    this.servers.values().stream().filter(target::contains).forEach(n -> b.put(n.getId(), n));
    return b.build();
  }

  public Map<UUID, NetworkNode> getServersOfNamespace(String namespace, String... namespaces) {
    Set<String> setN = Sets.newHashSet(namespaces);
    setN.add(namespace);
    setN.remove(null);
    return getServersOfNamespace(setN);
  }

  public Map<UUID, NetworkNode> getServersOfNamespace(Collection<String> namespaces) {
    Preconditions.checkNotNull(namespaces);
    ImmutableMap.Builder<UUID, NetworkNode> b = ImmutableMap.builder();
    this.servers.values().stream().filter(s -> s.hasNamespace(namespaces));
    return b.build();
  }

  public abstract <T, R> void sendProcedureCall(ProcedureCall<T, R> call);

  public abstract void sendProcedureResponse(ProcedureResponseMessage build);

  public abstract DataStore getDataStore();

  public abstract ProcedureManager getProcedureManager();

  public void publishChanges(HomeNode homeNode) {
    // TODO Auto-generated method stub

  }



}
