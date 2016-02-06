package de.rennschnitzel.net.core.procedure;

import java.util.Collection;
import java.util.function.BiConsumer;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.Node;
import de.rennschnitzel.net.core.Target;
import de.rennschnitzel.net.protocol.NetworkProtocol;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureCallMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureResponseMessage;
import de.rennschnitzel.net.util.function.CheckedFunction;
import lombok.Getter;

@Getter
public class CallableProcedure<T, R> extends Procedure {


  private @Getter final Class<T> argumentClass;
  private @Getter final Class<R> resultClass;

  private final CheckedFunction<ProcedureCallMessage, T> callReader;
  private final BiConsumer<ProcedureCallMessage.Builder, T> callWriter;
  private final CheckedFunction<ProcedureResponseMessage, R> responseReader;
  private final BiConsumer<ProcedureResponseMessage.Builder, R> responseWriter;

  private final AbstractNetwork network;

  public CallableProcedure(final AbstractNetwork network, final Procedure description, final Class<T> argClass,
      final Class<R> resultClass) {
    this(network, description.getName(), argClass, resultClass);
  }

  public CallableProcedure(final AbstractNetwork network, final String name, final Class<T> argClass, final Class<R> resultClass) {
    super(name, argClass, resultClass);
    Preconditions.checkNotNull(network);
    Preconditions.checkNotNull(argClass);
    Preconditions.checkNotNull(resultClass);
    this.network = network;
    this.argumentClass = argClass;
    this.resultClass = resultClass;

    // Compile reader / writer
    this.callReader = ProcedureUtils.compileCallReader(argClass);
    this.callWriter = ProcedureUtils.compileCallWriter(argClass);
    this.responseReader = ProcedureUtils.compileResponseReader(resultClass);
    this.responseWriter = ProcedureUtils.compileResponseWriter(resultClass);

  }

  @SuppressWarnings("unchecked")
  @Override
  public <A, B> CallableProcedure<A, B> bind(AbstractNetwork network, final Class<A> argumentType, final Class<B> resultType)
      throws IllegalArgumentException {
    if (argumentType.equals(this.getArgumentClass()) && resultType.equals(this.getResultClass())) {
      return (CallableProcedure<A, B>) this;
    }
    return super.bind(network, argumentType, resultType);
  }

  public <A, B> boolean isApplicable(CallableProcedure<A, B> procedure) {
    boolean result = true;
    result &= this.getName().equals(procedure.getName());
    result &= this.getArgumentClass().equals(procedure.getArgumentClass());
    result &= this.getResultClass().equals(procedure.getResultClass());
    return result;
  }

  public boolean isApplicable(Procedure procedure) {
    boolean result = true;
    result &= this.getName().equals(procedure.getName());
    result &= this.getArgumentClass().getName().equals(procedure.getArgumentType());
    result &= this.getResultClass().getName().equals(procedure.getResultType());
    return result;
  }

  public boolean isApplicable(NetworkProtocol.ProcedureDescription procedure) {
    boolean result = true;
    result &= this.getName().equals(procedure.getName());
    result &= this.getArgumentClass().getName().equals(procedure.getArgumentType());
    result &= this.getResultClass().getName().equals(procedure.getResultType());
    return result;
  }

  protected final void validate(NetworkProtocol.ProcedureDescription procedure) throws IllegalArgumentException {
    Preconditions.checkArgument(this.getName().equals(procedure.getName()));
    Preconditions.checkArgument(this.getArgumentClass().getName().equals(procedure.getArgumentType()));
    Preconditions.checkArgument(this.getResultClass().getName().equals(procedure.getResultType()));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof CallableProcedure)) {
      return false;
    }
    CallableProcedure<?, ?> o = (CallableProcedure<?, ?>) object;
    return compareTo(o) == 0;
  }

  public SingleProcedureCall<T, R> call(Node node, T argument) {
    return network.getProcedureManager().call(node, this, argument);
  }

  public SingleProcedureCall<T, R> call(Node node, T argument, long timeout) {
    return network.getProcedureManager().call(node, this, argument, timeout);
  }

  public MultiProcedureCall<T, R> call(Collection<Node> nodes, T argument) {
    return network.getProcedureManager().call(nodes, this, argument);
  }

  public MultiProcedureCall<T, R> call(Collection<Node> nodes, T argument, long timeout) {
    return network.getProcedureManager().call(nodes, this, argument, timeout);
  }

  public MultiProcedureCall<T, R> call(Target target, T argument) {
    return network.getProcedureManager().call(target, this, argument);
  }

  public MultiProcedureCall<T, R> call(Target target, T argument, long timeout) {
    return network.getProcedureManager().call(target, this, argument, timeout);
  }



}
