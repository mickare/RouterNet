package de.rennschnitzel.backbone.core;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Any;
import com.google.protobuf.MessageOrBuilder;

import de.rennschnitzel.backbone.ProtocolUtils;
import de.rennschnitzel.backbone.api.network.Network;
import de.rennschnitzel.backbone.api.network.Server;
import de.rennschnitzel.backbone.api.network.list.ServerCollection;
import de.rennschnitzel.backbone.api.network.target.Target;
import de.rennschnitzel.backbone.api.network.target.TargetOrBuilder;
import de.rennschnitzel.backbone.net.protocol.ComponentFstObject;
import de.rennschnitzel.backbone.net.protocol.ComponentUUID;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.ProcedureMessage;
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

  private final Map<UUID, Server> serversMap = Maps.newConcurrentMap();
  @Getter
  private final ServerCollection servers = new ServerCollection(serversMap);

  private final Set<String> namespacesSet = Sets.newConcurrentHashSet();
  @Getter
  private final Set<String> namespaces = Collections.unmodifiableSet(namespacesSet);


  private Server home;

  public AbstractNetwork(Logger logger) {
    Preconditions.checkNotNull(logger);
    this.logger = logger;


    Network.setInstance(this);
  }

  @Override
  public void sendBytes(TargetOrBuilder target, String key, byte[] data) {
    if (target.contains(home)) {

    }
  }

  @Override
  public void sendObject(TargetOrBuilder target, Object object) {
    // TODO Auto-generated method stub

  }

  @Override
  public void sendAny(TargetOrBuilder target, MessageOrBuilder message) {
    // TODO Auto-generated method stub

  }

  private void sendProcedure(TargetOrBuilder target, ProcedureContent content) {

  }


  private void sendProcedure(TargetOrBuilder target, ProcedureCallMessage call) {
    sendProcedure(target, ProcedureContent.newBuilder().setCall(call).build());
  }


  private void sendProcedure(TargetOrBuilder target, ProcedureResponseMessage response) {
    sendProcedure(target, ProcedureContent.newBuilder().setResponse(response).build());
  }

  @Override
  public <T, R> Map<Server, ListenableFuture<T>> callProcedures(TargetOrBuilder target, String name, Class<T> argumentType,
      Class<R> resultType) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void handle(TransportProtocol.ContentMessage message) throws ProtocolException {
    if (!Target.serverInTarget(message.getTarget(), home)) {
      // TODO - Dropped Target
      return;
    }
    switch (message.getContentCase()) {
      case ERROR:
        handle(message.getTarget(), message.getSender(), message.getError());
        break;
      case BYTES:
        handle(message.getTarget(), message.getSender(), message.getBytes());
        break;
      case OBJECTVALUE:
        handle(message.getTarget(), message.getSender(), message.getObjectValue());
        break;
      case PROTOVALUE:
        handle(message.getTarget(), message.getSender(), message.getProtoValue());
        break;
      case PROCEDURE:
        handle(message.getTarget(), message.getSender(), message.getProcedure());
        break;
      default:
        throw new ProtocolException("Invalid or unknown packet");
    }
  }

  private void handle(TransportProtocol.TargetMessage target, ComponentUUID.UUID sender, TransportProtocol.ErrorMessage error) {
    logger.log(error.getFatal() ? Level.SEVERE : Level.WARNING, "Remote Error on " + ProtocolUtils.convert(sender),
        error.getType() + " " + error.getMessage());
  }

  private void handle(TransportProtocol.TargetMessage target, ComponentUUID.UUID sender, TransportProtocol.ByteContent bytes) {
    receive(new Target(target), ProtocolUtils.convert(sender), bytes.getKey(), bytes.getData().toByteArray());
  }

  private void receive(Target target, UUID sender, String key, byte[] data) {

  }

  private void handle(TransportProtocol.TargetMessage target, ComponentUUID.UUID sender, ComponentFstObject.FstObject obj) {
    receive(new Target(target), ProtocolUtils.convert(sender), obj.getType(), FST.asObject(obj.getData().toByteArray()));
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
  public Set<Server> getServers(TargetOrBuilder target) {
    // TODO Auto-generated method stub
    return null;
  }



}
