package de.rennschnitzel.net.core.procedure;

import java.util.function.Function;

import de.rennschnitzel.net.core.AbstractNetwork;

public class CallableRegisteredProcedure<T, R> extends BoundProcedure<T, R> {

  public CallableRegisteredProcedure(AbstractNetwork network, CallableProcedure<T, R> procedure, Function<T, R> function) {
    super(network, procedure, function);
  }

  public CallableRegisteredProcedure(AbstractNetwork network, Procedure procedure, Class<T> argClass, Class<R> resultClass,
      Function<T, R> function) {
    super(network, procedure, argClass, resultClass, function);
  }

  public CallableRegisteredProcedure(AbstractNetwork network, String name, Class<T> argClass, Class<R> resultClass,
      Function<T, R> function) {
    super(network, name, argClass, resultClass, function);
  }


}
