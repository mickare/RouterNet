package de.rennschnitzel.backbone.api.network;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.MessageOrBuilder;

import de.rennschnitzel.backbone.api.network.list.ServerCollection;
import de.rennschnitzel.backbone.api.network.procedure.RegisteredProcedure;
import de.rennschnitzel.backbone.api.network.target.TargetOrBuilder;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;
import de.rennschnitzel.backbone.netty.exception.ProtocolException;

public interface NetworkInterface {

  ServerCollection getServers();

  Set<String> getNamespaces();
  
  void handle(TransportProtocol.Message message) throws ProtocolException;
  
  void sendBytes(TargetOrBuilder target, String key, byte[] data);

  void sendObject(TargetOrBuilder target, Object object);

  void sendAny(TargetOrBuilder target, MessageOrBuilder message);

  <T, R> Map<Server, ListenableFuture<T>> callProcedures(TargetOrBuilder target, String name, Class<T> argument, Class<R> result);

  <T, R> RegisteredProcedure<T, R> registerProcedure(String name, Class<T> argument, Class<R> result, Function<T, ? extends R> function);

  Set<RegisteredProcedure<?, ?>> getRegisteredProcedures();

  <T, R> RegisteredProcedure<T, R> getRegisteredProcedure(String name, Class<T> argument, Class<R> result);

}
