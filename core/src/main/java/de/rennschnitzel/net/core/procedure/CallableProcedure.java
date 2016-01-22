package de.rennschnitzel.net.core.procedure;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

import com.google.common.util.concurrent.ListenableFuture;

import de.rennschnitzel.net.core.Node;
import de.rennschnitzel.net.protocol.NetworkProtocol;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureCallMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureResponseMessage;
import de.rennschnitzel.net.util.function.CheckedFunction;

public interface CallableProcedure<T, R> extends Comparable<CallableProcedure<?, ?>> {

  /**
   * Gets name of the procedure
   * 
   * @return name of procedure
   */
  String getName();

  /**
   * Gets the description of the procedure that defines this callable.
   * 
   * @return description of procedure
   */
  Procedure getDescription();

  /**
   * Gets the argument class.
   * 
   * @return class of argument
   */
  Class<T> getArgClass();

  /**
   * Gets the result object class.
   * 
   * @return class of result
   */
  Class<R> getResultClass();

  /**
   * 
   * @param procedure
   * @return
   */
  boolean isApplicable(NetworkProtocol.ProcedureDescription procedure);

  CheckedFunction<ProcedureCallMessage, T> getCallReader();

  BiConsumer<ProcedureCallMessage.Builder, T> getCallWriter();

  CheckedFunction<ProcedureResponseMessage, R> getResponseReader();

  BiConsumer<ProcedureResponseMessage.Builder, R> getResponseWriter();

  boolean isApplicable(Procedure info);

  ProcedureCallResult<T, R> call(Node node, T argument);

  ProcedureCallResult<T, R> call(Node node, T argument, long timeout);

  Map<UUID, ? extends ListenableFuture<R>> call(Collection<Node> nodes, T argument, long timeout);

  Map<UUID, ? extends ListenableFuture<R>> call(Collection<Node> nodes, T argument);

}
