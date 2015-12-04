package de.rennschnitzel.backbone.api.network.procedure;

import java.util.function.BiConsumer;

import com.google.common.util.concurrent.ListenableFuture;

import de.rennschnitzel.backbone.net.protocol.NetworkProtocol;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureCall;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureResponse;
import de.rennschnitzel.backbone.util.CheckedFunction;

public interface Procedure<T, R> extends Comparable<Procedure<?, ?>> {

  String getName();

  String getArgClassName();

  Class<T> getArgClass();

  String getResultClassName();

  Class<R> getResultClass();

  ListenableFuture<R> call(T arg);


  default NetworkProtocol.Procedure toProtocol() {
    return NetworkProtocol.Procedure.newBuilder().setName(getName()).setArgument(getArgClassName()).setResult(getResultClassName()).build();
  }


  CheckedFunction<ProcedureCall, T> getCallReader();

  BiConsumer<ProcedureCall.Builder, T> getCallWriter();

  CheckedFunction<ProcedureResponse, R> getResponseReader();

  BiConsumer<ProcedureResponse.Builder, R> getResponseWriter();

}
