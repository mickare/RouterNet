package de.rennschnitzel.net.core.procedure;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureCallMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureResponseMessage;
import de.rennschnitzel.net.util.TypeUtils;
import lombok.Getter;

public class BoundProcedure<T, R> extends CallableProcedure<T, R> {


  // ***************************************************************************
  // STATIC - START

  @SuppressWarnings("unchecked")
  public static <T, R> BoundProcedure<T, R> of(String name, Function<T, R> function) {
    Class<?>[] typeArgs = TypeUtils.resolveArgumentClass(function);
    return new BoundProcedure<T, R>(AbstractNetwork.getInstance(), name, (Class<T>) typeArgs[0], (Class<R>) typeArgs[1], function);
  }

  public static <T> BoundProcedure<T, Void> of(String name, Consumer<T> consumer) {
    return new BoundProcedure<T, Void>(AbstractNetwork.getInstance(), name, TypeUtils.resolveArgumentClass(consumer), Void.class,
        t -> {
          consumer.accept(t);
          return null;
        });
  }


  public static <R> BoundProcedure<Void, R> of(String name, Supplier<R> supplier) {
    return new BoundProcedure<Void, R>(AbstractNetwork.getInstance(), name, Void.class, TypeUtils.resolveArgumentClass(supplier),
        t -> supplier.get());
  }


  public static BoundProcedure<Void, Void> of(String name, Runnable runnable) {
    return new BoundProcedure<Void, Void>(AbstractNetwork.getInstance(), name, Void.class, Void.class, t -> {
      runnable.run();
      return null;
    });
  }

  // STATIC - END
  // ***************************************************************************


  @Getter
  private final Function<T, R> function;

  public BoundProcedure(AbstractNetwork network, CallableProcedure<T, R> procedure, Function<T, R> function) {
    this(network, procedure.getName(), procedure.getArgumentClass(), procedure.getResultClass(), function);
  }

  public BoundProcedure(AbstractNetwork network, Procedure procedure, Class<T> argClass, Class<R> resultClass,
      Function<T, R> function) {
    this(network, procedure.checkApplicable(argClass, resultClass).getName(), argClass, resultClass, function);
  }

  public BoundProcedure(AbstractNetwork network, String name, Class<T> argClass, Class<R> resultClass, Function<T, R> function) {
    super(network, name, argClass, resultClass);
    Preconditions.checkNotNull(function);
    this.function = function;
  }

  public R call(T arg) {
    return this.function.apply(arg);
  }

  public R remoteCalled(ProcedureCallMessage call, ProcedureResponseMessage.Builder out) throws Exception {
    R result = this.call(this.getCallReader().apply(call));
    this.getResponseWriter().accept(out, result);
    return result;
  }

}
