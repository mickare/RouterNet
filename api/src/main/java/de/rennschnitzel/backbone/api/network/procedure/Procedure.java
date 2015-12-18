package de.rennschnitzel.backbone.api.network.procedure;

import java.util.function.BiConsumer;

import de.rennschnitzel.backbone.net.node.NetworkNode;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureCallMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureResponseMessage;
import de.rennschnitzel.backbone.util.function.CheckedFunction;

public interface Procedure<T, R> extends Comparable<Procedure<?, ?>> {

  String getName();

  ProcedureInformation getInfo();

  Class<T> getArgClass();

  Class<R> getResultClass();

  default NetworkProtocol.ProcedureDescription toProtocol() {
    return NetworkProtocol.ProcedureDescription.newBuilder().setName(getName()).setArgument(getArgClass().getName())
        .setResult(getResultClass().getName()).build();
  }

  boolean isApplicable(NetworkProtocol.ProcedureDescription procedure);

  CheckedFunction<ProcedureCallMessage, T> getCallReader();

  BiConsumer<ProcedureCallMessage.Builder, T> getCallWriter();

  CheckedFunction<ProcedureResponseMessage, R> getResponseReader();

  BiConsumer<ProcedureResponseMessage.Builder, R> getResponseWriter();

  CheckedFunction<T, R> getLocalFunction();

  void setLocalFunction(CheckedFunction<T, R> function);

  boolean isLocalFunction();

  ProcedureCallResult<T, R> call(NetworkNode server, T argument);

}
