package de.rennschnitzel.backbone.api.network.procedure;

import java.util.function.BiConsumer;

import com.google.common.util.concurrent.ListenableFuture;

import de.rennschnitzel.backbone.net.protocol.NetworkProtocol;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;

public interface Procedure<T, R> {

  String getName();

  String getArgClassName();

  Class<T> getArgClass();

  String getResultClassName();

  Class<R> getResultClass();

  ListenableFuture<R> call(T arg);

  BiConsumer<TransportProtocol.ProcedureCall.Builder, T> getArgumentWriter();

  default NetworkProtocol.Procedure toProtocol() {
    return NetworkProtocol.Procedure.newBuilder().setName(getName()).setArgument(getArgClassName()).setResult(getResultClassName()).build();
  }

}
