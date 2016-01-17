package de.rennschnitzel.net.core.procedure;

import java.util.function.BiConsumer;

import de.rennschnitzel.net.core.Node;
import de.rennschnitzel.net.protocol.NetworkProtocol;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureCallMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureResponseMessage;
import de.rennschnitzel.net.util.function.CheckedFunction;

public interface Procedure<T, R> extends Comparable<Procedure<?, ?>> {

  String getName();

  ProcedureInformation getInfo();

  Class<T> getArgClass();

  Class<R> getResultClass();

  default NetworkProtocol.ProcedureDescription toProtocol() {
    return NetworkProtocol.ProcedureDescription.newBuilder().setName(getName()).setArgumentType(getArgClass().getName())
        .setResultType(getResultClass().getName()).build();
  }

  boolean isApplicable(NetworkProtocol.ProcedureDescription procedure);

  CheckedFunction<ProcedureCallMessage, T> getCallReader();

  BiConsumer<ProcedureCallMessage.Builder, T> getCallWriter();

  CheckedFunction<ProcedureResponseMessage, R> getResponseReader();

  BiConsumer<ProcedureResponseMessage.Builder, R> getResponseWriter();

  CheckedFunction<T, R> getLocalFunction();

  void setLocalFunction(CheckedFunction<T, R> function);

  boolean isLocalFunction();

  ProcedureCallResult<T, R> call(Node node, T argument);

  boolean isApplicable(ProcedureInformation info);

}
