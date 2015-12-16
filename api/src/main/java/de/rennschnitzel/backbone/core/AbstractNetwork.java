package de.rennschnitzel.backbone.core;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;

import de.rennschnitzel.backbone.ProtocolUtils;
import de.rennschnitzel.backbone.api.network.Connection;
import de.rennschnitzel.backbone.api.network.MessageEventBus;
import de.rennschnitzel.backbone.api.network.Network;
import de.rennschnitzel.backbone.api.network.Server;
import de.rennschnitzel.backbone.api.network.message.ByteMessage;
import de.rennschnitzel.backbone.api.network.message.ObjectMessage;
import de.rennschnitzel.backbone.api.network.message.PackableMessage;
import de.rennschnitzel.backbone.api.network.target.Target;
import de.rennschnitzel.backbone.api.network.target.TargetOrBuilder;
import de.rennschnitzel.backbone.net.protocol.ComponentUUID;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ErrorMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureCallMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureContent;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureResponseMessage;
import de.rennschnitzel.backbone.netty.exception.ProtocolException;
import lombok.Getter;

public class AbstractNetwork extends Network {

  @Getter
  private final Logger logger;

  private final Map<UUID, Server> servers = Maps.newConcurrentMap();

  private final Set<String> namespacesSet = Sets.newConcurrentHashSet();
  @Getter
  private final Set<String> namespaces = Collections.unmodifiableSet(namespacesSet);

  @Getter
  private final MessageEventBus messageEventBus = new MessageEventBus();

  private Server home;

  public AbstractNetwork(Logger logger) {
    Preconditions.checkNotNull(logger);
    this.logger = logger;


    Network.setInstance(this);
  }

  @Override
  public void sendBytes(TargetOrBuilder target, String key, byte[] data) {
    Preconditions.checkArgument(!key.isEmpty());
    final Target t = target.build();
    Preconditions.checkArgument(!t.isEmpty());
    final String k = key.toLowerCase();

    final boolean containsHome = t.contains(home);

    ByteMessage msg = new ByteMessage(t, home.getID(), key, data);
    
    if (t.isToAll() || !t.getNamespaces().isEmpty() || t.getServers().size() > (containsHome ? 1 : 0)) {
      write(msg);
    }

    if (containsHome) {
      this.handle(msg);
    }
  }

  protected abstract void write(PackableMessage packet);

  protected abstract void writeProcedure(TargetOrBuilder target, ProcedureContent content);

  @Override
  public void sendObject(TargetOrBuilder target, Object object) {
    Preconditions.checkNotNull(object);
    Preconditions.checkArgument(object instanceof Serializable, "object is not serializable");
    final Target t = target.build();
    Preconditions.checkArgument(!t.isEmpty());

  }

  private void sendProcedure(TargetOrBuilder target, ProcedureCallMessage call) {
    writeProcedure(target, ProcedureContent.newBuilder().setCall(call).build());
  }


  private void sendProcedure(TargetOrBuilder target, ProcedureResponseMessage response) {
    writeProcedure(target, ProcedureContent.newBuilder().setResponse(response).build());
  }

  @Override
  public void handle(TransportProtocol.ContentMessage message) throws ProtocolException {
    if (!Target.serverInTarget(message.getTarget(), home)) {
      // TODO - Dropped Target
      return;
    }
    switch (message.getContentCase()) {
      case BYTES:
        handle(new ByteMessage(message));
        break;
      case OBJECT:
        handle(new ObjectMessage(message));
        handle(message.getTarget(), message.getSender(), message.getObject());
        break;
      case PROCEDURE:
        handle(message.getTarget(), message.getSender(), message.getProcedure());
        break;
      default:
        throw new ProtocolException("Invalid or unknown packet");
    }
  }

  private void handle(ObjectMessage msg) {
    this.messageEventBus.callListeners(msg);
  }

  private void handle(ByteMessage msg) {
    this.messageEventBus.callListeners(msg);
  }

  private void handle(TransportProtocol.TargetMessage target, ComponentUUID.UUID sender, TransportProtocol.ObjectContent object) {
    receive(new Target(target), ProtocolUtils.convert(sender), object.getType(), FST.asObject(object.getData().toByteArray()));
  }

  private void receive(Target target, UUID sender, String type, Object asObject) {

  }

  private void handle(TransportProtocol.TargetMessage target, ComponentUUID.UUID sender, com.google.protobuf.Any any) {
    receive(new Target(target), ProtocolUtils.convert(sender), any);
  }

  private void receive(Target target, UUID sender, Any any) {
    // TODO Auto-generated method stub

  }

  private void handle(TransportProtocol.TargetMessage target, ComponentUUID.UUID sender, ProcedureContent procedure)
      throws ProtocolException {
    receive(new Target(target), ProtocolUtils.convert(sender), procedure);
  }

  private void receive(Target target, UUID sender, ProcedureContent procedure) throws ProtocolException {
    switch (procedure.getContentCase()) {
      case CALL:
        receive(target, sender, procedure.getCall());
        break;
      case RESPONSE:
        receive(target, sender, procedure.getResponse());
        break;
      default:
        throw new ProtocolException("Invalid or unknown packet");
    }
  }

  private static ProcedureResponseMessage.Builder newProcedureResponse(final ProcedureCallMessage call) {
    ProcedureResponseMessage.Builder b = ProcedureResponseMessage.newBuilder();
    b.setProcedure(call.getProcedure());
    b.setId(call.getId());
    b.setTimestamp(call.getTimestamp());
    return b;
  }


  private void receive(Target target, UUID sender, ProcedureCallMessage call) {
    // RegisteredProcedure<?, ?> proc = getRegisteredProcedure(call.getProcedure());
    // if (proc == null) {
    // sendProcedureError(sender, call,
    //// return;
    // }
    // sendProcedure(Target.toServer(sender), proc.call(call));
  }


  private void sendProcedureError(UUID sender, ProcedureCallMessage call, ErrorMessage error) {
    ProcedureResponseMessage.Builder b = newProcedureResponse(call);
    b.setSuccess(false);
    b.setCancelled(false);
    b.setError(error);
    sendProcedure(Target.toServer(sender), b.build());
  }


  private void receive(Target target, UUID sender, ProcedureResponseMessage response) {
    // TODO Auto-generated method stub

  }

  @Override
  public UUID getID() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Map<UUID, Server> getServers() {
    return Collections.unmodifiableMap(this.servers);
  }

  @Override
  public Map<UUID, Server> getServers(TargetOrBuilder target) {
    ImmutableMap.Builder<UUID, Server> b = ImmutableMap.builder();
    final Target t = target.build();
    if (t.isToAll()) {
      b.putAll(this.servers);
    } else {
      for (Server server : this.servers.values()) {
        if (target.contains(server)) {
          b.put(server.getID(), server);
        }
      }
    }
    return b.build();
  }

}
