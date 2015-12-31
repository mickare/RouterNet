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

import de.rennschnitzel.backbone.ProtocolUtils;
import de.rennschnitzel.backbone.net.channel.ChannelDescriptors;
import de.rennschnitzel.backbone.net.channel.object.ObjectChannel;
import de.rennschnitzel.backbone.net.channel.stream.StreamChannel;
import de.rennschnitzel.backbone.net.node.BaseNetworkNode;
import de.rennschnitzel.backbone.net.node.HomeNode;
import de.rennschnitzel.backbone.net.node.NetworkNode;
import de.rennschnitzel.backbone.net.procedure.ProcedureCall;
import de.rennschnitzel.backbone.net.protocol.ComponentUUID;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.ConnectedMessage;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.DisconnectedMessage;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.ServerMessage;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.ServerUpdateMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureResponseMessage;
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

  public static StreamChannel.Descriptor getStreamChannel(String name) {
    return ChannelDescriptors.getStreamChannel(name);
  }

  public static StreamChannel.Descriptor getStreamChannel(String name, int bufferSize) {
    return ChannelDescriptors.getStreamChannel(name, bufferSize);
  }


  // static end
  // ******************************************************************************************


  private final ConcurrentMap<UUID, NetworkNode> nodes = new ConcurrentHashMap<>();

  @Getter
  private final EventBus eventBus = new EventBus();


  public abstract HomeNode getHome();

  public abstract Logger getLogger();

  protected void setInstance() {
    Network.instance = this;
  }

  public Map<UUID, NetworkNode> getNodes() {
    return Collections.unmodifiableMap(nodes);
  }

  public Map<UUID, NetworkNode> getServersOfTarget(Target target) {
    ImmutableMap.Builder<UUID, NetworkNode> b = ImmutableMap.builder();
    this.nodes.values().stream().filter(target::contains).forEach(n -> b.put(n.getId(), n));
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
    this.nodes.values().stream().filter(s -> s.hasNamespace(namespaces));
    return b.build();
  }

  public abstract <T, R> void sendProcedureCall(ProcedureCall<T, R> call);


  public void sendProcedureResponse(ComponentUUID.UUID receiver, ProcedureResponseMessage build) {
    sendProcedureResponse(ProtocolUtils.convert(receiver), build);
  }

  public abstract void sendProcedureResponse(UUID receiver, ProcedureResponseMessage build);

  public abstract ProcedureManager getProcedureManager();

  public void publishChanges(HomeNode homeNode) {
    // TODO Auto-generated method stub

  }

  public void updateNodes(ServerMessage msg) {
    UUID id = ProtocolUtils.convert(msg.getId());
    this.nodes.computeIfAbsent(id, BaseNetworkNode::new).update(msg);
  }

  public void updateNodes(ConnectedMessage msg) {
    updateNodes(msg.getServer());
  }

  public void updateNodes(ServerUpdateMessage msg) {
    updateNodes(msg.getServer());
  }

  public void updateNodes(DisconnectedMessage msg) {
    UUID id = ProtocolUtils.convert(msg.getServer().getId());
    Preconditions.checkArgument(!id.equals(this.getHome().getId()));
    NetworkNode node = this.nodes.get(id);
    if (node != null) {
      node.update(msg.getServer());
      this.nodes.remove(id, node);
    }
  }



}
