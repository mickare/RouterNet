package de.rennschnitzel.backbone.api.network;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.google.common.util.concurrent.ListenableFuture;

import de.rennschnitzel.backbone.api.network.procedure.Procedure;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol;

public interface Server {

  UUID getID();

  Optional<String> getName();

  Set<String> getNamespaces();

  NetworkProtocol.Server.Type getType();

  boolean isConnected();

  /**
   * Gets the registered procedures of the server.
   * 
   * @return set of services
   */
  Set<Procedure<?, ?>> getServices();

  /**
   * Gets a registered procedure of a server.
   * 
   * @param name of service
   * @param argument class type
   * @param result class type
   * @return null if not a registered service.
   */
  <T, R> Procedure<T, R> getProcedure(String name, Class<T> argument, Class<R> result);

  <T, R> ListenableFuture<R> call(Procedure<T, R> remoteProcedure, T arg);

  Connection getConnection();

  boolean hasNamespace(String namespace);

}
