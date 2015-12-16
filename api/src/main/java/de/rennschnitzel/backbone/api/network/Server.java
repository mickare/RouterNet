package de.rennschnitzel.backbone.api.network;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.BiMap;

import de.rennschnitzel.backbone.api.network.procedure.Procedure;
import de.rennschnitzel.backbone.api.network.procedure.ProcedureCallResult;
import de.rennschnitzel.backbone.api.network.procedure.ProcedureInformation;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol;

public interface Server extends MessageReceiver {

  UUID getID();

  Optional<String> getName();

  Set<String> getNamespaces();

  NetworkProtocol.ServerMessage.Type getType();

  boolean isConnected();

  /**
   * Gets the procedures of the server.
   * 
   * @return map of procedures
   */
  BiMap<ProcedureInformation, Procedure<?, ?>> getProcedures();

  /**
   * Gets a registered procedure of a server.
   * 
   * @param name of service
   * @param argument class type
   * @param result class type
   * @return null if not a registered service.
   */
  <T, R> Procedure<T, R> getProcedure(String name, Class<T> argument, Class<R> result);

  <T, R> ProcedureCallResult<T, R> call(Procedure<T, R> procedure, T argument);

  boolean hasNamespace(String namespace);

  boolean hasProcedure(ProcedureInformation info);

}
