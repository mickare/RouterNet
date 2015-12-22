package de.rennschnitzel.backbone.net.procedure;

import java.util.function.Function;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureCallMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureResponseMessage;
import lombok.Getter;
import net.jodah.typetools.TypeResolver;

public class RegisteredProcedure<T, R> extends BaseProcedure<T, R> {

  private static <T, R> Class<?>[] resolveArgumentClass(Function<T, R> function) {
    Preconditions.checkNotNull(function);
    return TypeResolver.resolveRawArguments(Function.class, function.getClass());
  }

  @Getter
  private final Function<T, R> function;

  public RegisteredProcedure(String name, Function<T, R> function) {
    this(name, resolveArgumentClass(function), function);
  }

  @SuppressWarnings("unchecked")
  private RegisteredProcedure(String name, Class<?>[] typeArgs, Function<T, R> function) {
    this(name, (Class<T>) typeArgs[0], (Class<R>) typeArgs[1], function);
  }

  public RegisteredProcedure(String name, Class<T> argClass, Class<R> resultClass, Function<T, R> function) {
    this(new ProcedureInformation(name, argClass.getName(), resultClass.getName()), argClass, resultClass, function);
  }

  public RegisteredProcedure(ProcedureInformation info, Class<T> argClass, Class<R> resultClass, Function<T, R> function) {
    super(info, argClass, resultClass);
    Preconditions.checkNotNull(function);
    this.function = function;
  }

  public R call(T arg) {
    return this.function.apply(arg);
  }

  public void call(ProcedureCallMessage call, ProcedureResponseMessage.Builder out) throws Exception {
    R result = this.call(this.getCallReader().apply(call));
    this.getResponseWriter().accept(out, result);
  }

}
