package de.rennschnitzel.backbone.core;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.nustaq.serialization.FSTConfiguration;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.MessageOrBuilder;

import de.rennschnitzel.backbone.ProtocolUtils;
import de.rennschnitzel.backbone.api.network.Network;
import de.rennschnitzel.backbone.api.network.Server;
import de.rennschnitzel.backbone.api.network.list.ServerCollection;
import de.rennschnitzel.backbone.api.network.procedure.RegisteredProcedure;
import de.rennschnitzel.backbone.api.network.target.Target;
import de.rennschnitzel.backbone.api.network.target.TargetOrBuilder;
import de.rennschnitzel.backbone.net.protocol.ComponentFstObject;
import de.rennschnitzel.backbone.net.protocol.ComponentUUID;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.Procedure;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ByteContent;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ErrorMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureCall;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureContent;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureResponse;
import de.rennschnitzel.backbone.netty.exception.ProtocolException;
import lombok.Getter;

public class AbstractNetwork extends Network {


  private final Map<UUID, Server> serversMap = Maps.newConcurrentMap();
  @Getter
  private final ServerCollection servers = new ServerCollection(serversMap);

  private final Set<String> namespacesSet = Sets.newConcurrentHashSet();
  @Getter
  private final Set<String> namespaces = Collections.unmodifiableSet(namespacesSet);


  private Server home;

  public AbstractNetwork() {
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

  private void sendProcedureResponse(TargetOrBuilder target, TransportProtocol.ProcedureResponse response) {
    sendProcedure(target, ProcedureContent.newBuilder().setResponse(response).build());
  }

  private void sendProcedureCall(TargetOrBuilder target, TransportProtocol.ProcedureCall call) {
    sendProcedure(target, ProcedureContent.newBuilder().setCall(call).build());
  }

  @Override
  public <T, R> Map<Server, ListenableFuture<T>> callProcedures(TargetOrBuilder target, String name, Class<T> argumentType,
      Class<R> resultType) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <T, R> RegisteredProcedure<T, R> registerProcedure(String name, Class<T> argumentType, Class<R> resultType,
      Function<T, ? extends R> function) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Set<RegisteredProcedure<?, ?>> getRegisteredProcedures() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <T, R> RegisteredProcedure<T, R> getRegisteredProcedure(String name, Class<T> argumentType, Class<R> resultType) {
    // TODO Auto-generated method stub
    return null;
  }

  public <T, R> RegisteredProcedure<T, R> getRegisteredProcedure(String name, String argumentType, String resultType) {
    // TODO Auto-generated method stub
    return null;
  }

  private RegisteredProcedure<?, ?> getRegisteredProcedure(Procedure procedure) {
    return getRegisteredProcedure(procedure.getName(), procedure.getArgument(), procedure.getResult());
  }

  @Override
  public void handle(TransportProtocol.Message message) throws ProtocolException {
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

  private void handle(TransportProtocol.Target target, ComponentUUID.UUID uuid, TransportProtocol.ErrorMessage error) {

  }

  private void handle(TransportProtocol.Target target, ComponentUUID.UUID sender, TransportProtocol.ByteContent bytes) {
    receive(new Target(target), ProtocolUtils.convert(sender), bytes.getKey(), bytes.getData().toByteArray());
  }

  private void receive(Target target, UUID sender, String key, byte[] data) {

  }

  private void handle(TransportProtocol.Target target, ComponentUUID.UUID sender, ComponentFstObject.FstObject obj) {
    receive(new Target(target), ProtocolUtils.convert(sender), obj.getType(), fst.asObject(obj.getData().toByteArray()));
  }

  private void receive(Target target, UUID sender, String type, Object asObject) {

  }

  private void handle(TransportProtocol.Target target, ComponentUUID.UUID sender, com.google.protobuf.Any any) {
    receive(new Target(target), ProtocolUtils.convert(sender), any);
  }

  private void receive(Target target, UUID sender, Any any) {
    // TODO Auto-generated method stub

  }

  private void handle(TransportProtocol.Target target, ComponentUUID.UUID sender, ProcedureContent procedure) throws ProtocolException {
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

  private void receive(Target target, UUID sender, ProcedureCall call) {
    RegisteredProcedure<?, ?> proc = getRegisteredProcedure(call.getProcedure());
    if (proc == null) {
      sendProcedureError(sender, call,
          ErrorMessage.newBuilder().setType(ErrorMessage.Type.UNDEFINED).setMessage("Procedure not found!").build());
      return;
    }
    sendProcedureResponse(Target.toServer(sender), proc.call(call));
  }


  private void sendProcedureError(UUID sender, ProcedureCall call, ErrorMessage error) {
    ProcedureResponse.Builder b = ProcedureResponse.newBuilder();
    b.setProcedure(call.getProcedure());
    b.setId(call.getId());
    b.setSuccess(false);
    b.setCancelled(false);
    b.setError(error);
    sendProcedureResponse(Target.toServer(sender), b.build());
  }


  private void receive(Target target, UUID sender, ProcedureResponse response) {
    // TODO Auto-generated method stub

  }



}
