package de.rennschnitzel.backbone.api.network.procedure;

import java.util.function.BiConsumer;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import de.rennschnitzel.backbone.api.network.Network;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ErrorMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureCall;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureResponse;
import de.rennschnitzel.backbone.util.CheckedFunction;
import lombok.Getter;


@Getter
public class RegisteredProcedure<T, R> implements Procedure<T, R> {

  private final String name;
  private Class<T> argClass;
  private Class<R> resultClass;
  private final CheckedFunction<T, R> function;

  private final CheckedFunction<ProcedureCall, T> argumentReader;
  private final BiConsumer<ProcedureResponse.Builder, R> resultWriter;

  @SuppressWarnings("unchecked")
  public RegisteredProcedure(final String name, final Class<T> argClass, final Class<R> resultClass, final CheckedFunction<T, R> function) {
    Preconditions.checkArgument(!name.isEmpty());
    Preconditions.checkNotNull(argClass);
    Preconditions.checkNotNull(resultClass);
    Preconditions.checkNotNull(function);
    this.name = name;
    this.argClass = argClass;
    this.resultClass = resultClass;
    this.function = function;

    // Compile extractor

    if (Void.class.equals(argClass)) {
      argumentReader = (p) -> null;
    } else if (byte[].class.equals(argClass)) {
      argumentReader = (p) -> (T) p.getBytes().toByteArray();
    } else if (Any.class.equals(argClass)) {
      argumentReader = (p) -> (T) p.getProtoValue();
    } else if (com.google.protobuf.Message.class.isAssignableFrom(argClass)) {
      argumentReader = (p) -> (T) p.getProtoValue().unpack((Class<? extends com.google.protobuf.Message>) argClass);
    } else if (Object.class.isAssignableFrom(argClass)) {
      argumentReader = (p) -> (T) Network.FST.getObjectInput(p.getObjectValue().toByteArray()).readObject();
    } else {
      throw new IllegalArgumentException("Unsupported argument type!");
    }

    // Compile writer
    
    if (Void.class.equals(resultClass)) {
      resultWriter = (p, r) -> p.clearData();
    } else if (byte[].class.equals(resultClass)) {
      resultWriter = (p, r) -> p.setBytes(ByteString.copyFrom((byte[]) r));
    } else if (Any.class.equals(resultClass)) {
      resultWriter = (p, r) -> p.setProtoValue((Any) r);
    } else if (com.google.protobuf.Message.class.isAssignableFrom(resultClass)) {
      resultWriter = (p, r) -> p.setProtoValue(Any.pack((Message) r));
    } else if (Object.class.isAssignableFrom(resultClass)) {
      resultWriter = (p, r) -> p.setObjectValue(ByteString.copyFrom(Network.FST.asByteArray(r)));
    } else {
      throw new IllegalArgumentException("Unsupported result type!");
    }

  }

  @Override
  public ListenableFuture<R> call(T arg) {
    try {
      return Futures.immediateFuture(function.apply(arg));
    } catch (Exception e) {
      return Futures.immediateFailedFuture(e);
    }
  }

  @Override
  public String getArgClassName() {
    return argClass.getName();
  }

  @Override
  public String getResultClassName() {
    return resultClass.getName();
  }

  private void validate(NetworkProtocol.Procedure procedure) throws IllegalArgumentException {
    Preconditions.checkArgument(this.name.equals(procedure.getName()));
    Preconditions.checkArgument(getArgClassName().equals(procedure.getArgument()));
    Preconditions.checkArgument(getResultClassName().equals(procedure.getResult()));
  }

  public ProcedureResponse call(ProcedureCall call) {
    ProcedureResponse.Builder b = ProcedureResponse.newBuilder();
    b.setProcedure(call.getProcedure());
    b.setId(call.getId());
    b.setTimestamp(call.getTimestamp());
    try {
      validate(call.getProcedure());
      this.resultWriter.accept(b, function.apply(argumentReader.apply(call)));
      b.setSuccess(true);
    } catch (Exception ex) {
      b.setSuccess(false);
      b.setError(ErrorMessage.newBuilder().setType(ErrorMessage.Type.UNDEFINED).setMessage(ex.getMessage()));
    }
    return b.build();
  }

}
