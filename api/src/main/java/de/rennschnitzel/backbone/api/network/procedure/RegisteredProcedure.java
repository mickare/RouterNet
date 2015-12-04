package de.rennschnitzel.backbone.api.network.procedure;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ErrorMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureCall;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureResponse;
import de.rennschnitzel.backbone.util.CheckedFunction;
import lombok.Getter;


@Getter
public class RegisteredProcedure<T, R> extends AbstractProcedure<T, R> {

  private final CheckedFunction<T, R> function;

  public RegisteredProcedure(final String name, final Class<T> argClass, final Class<R> resultClass, final CheckedFunction<T, R> function) {
    super(name, argClass, resultClass);
    Preconditions.checkNotNull(function);
    this.function = function;
  }

  @Override
  public ListenableFuture<R> call(T arg) {
    try {
      return Futures.immediateFuture(function.apply(arg));
    } catch (Exception e) {
      return Futures.immediateFailedFuture(e);
    }
  }

  public ProcedureResponse call(ProcedureCall call) {
    ProcedureResponse.Builder b = ProcedureResponse.newBuilder();
    b.setProcedure(call.getProcedure());
    b.setId(call.getId());
    b.setTimestamp(call.getTimestamp());
    try {
      validate(call.getProcedure());
      this.getResponseWriter().accept(b, function.apply(this.getCallReader().apply(call)));
      b.setSuccess(true);
    } catch (Exception ex) {
      b.setSuccess(false);
      b.setError(ErrorMessage.newBuilder().setType(ErrorMessage.Type.UNDEFINED).setMessage(ex.getMessage()));
    }
    return b.build();
  }



}
