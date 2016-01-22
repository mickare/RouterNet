package de.rennschnitzel.net.core.procedure;

import java.util.function.Function;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureCallMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureResponseMessage;
import lombok.Getter;

public class CallableRegisteredProcedure<T, R> extends CallableBaseProcedure<T, R> {

  @Getter
  private final Function<T, R> function;

  /*
   * public RegisteredProcedure(Network network, String name, Function<T, R> function) {
   * this(network, name, resolveArgumentClass(function), function); }
   * 
   * @SuppressWarnings("unchecked") private RegisteredProcedure(Network network, String name,
   * Class<?>[] typeArgs, Function<T, R> function) { this(network, name, (Class<T>) typeArgs[0],
   * (Class<R>) typeArgs[1], function); }
   */

  public CallableRegisteredProcedure(AbstractNetwork network, String name, Class<T> argClass, Class<R> resultClass, Function<T, R> function) {
    this(network, new Procedure(name, argClass.getName(), resultClass.getName()), argClass, resultClass, function);
  }

  public CallableRegisteredProcedure(AbstractNetwork network, Procedure info, Class<T> argClass, Class<R> resultClass, Function<T, R> function) {
    super(network, info, argClass, resultClass);
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
