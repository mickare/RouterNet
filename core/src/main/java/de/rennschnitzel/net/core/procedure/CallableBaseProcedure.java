package de.rennschnitzel.net.core.procedure;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.Node;
import de.rennschnitzel.net.protocol.NetworkProtocol;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureCallMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureResponseMessage;
import de.rennschnitzel.net.util.function.CheckedFunction;
import lombok.Getter;

@Getter
public class CallableBaseProcedure<T, R> implements CallableProcedure<T, R>, Comparable<CallableProcedure<?, ?>>  {

  private final Procedure description;
  private final Class<T> argClass;
  private final Class<R> resultClass;
  private final CheckedFunction<ProcedureCallMessage, T> callReader;
  private final BiConsumer<ProcedureCallMessage.Builder, T> callWriter;
  private final CheckedFunction<ProcedureResponseMessage, R> responseReader;
  private final BiConsumer<ProcedureResponseMessage.Builder, R> responseWriter;

  private final AbstractNetwork network;

  public CallableBaseProcedure(final AbstractNetwork network, final Procedure description, final Class<T> argClass, final Class<R> resultClass) {
    Preconditions.checkNotNull(network);
    Preconditions.checkNotNull(description);
    Preconditions.checkNotNull(argClass);
    Preconditions.checkNotNull(resultClass);

    this.network = network;
    this.description = description;
    this.argClass = argClass;
    this.resultClass = resultClass;

    // Compile reader / writer
    this.callReader = ProcedureUtils.compileCallReader(argClass);
    this.callWriter = ProcedureUtils.compileCallWriter(argClass);
    this.responseReader = ProcedureUtils.compileResponseReader(resultClass);
    this.responseWriter = ProcedureUtils.compileResponseWriter(resultClass);

  }

  @Override
  public String getName() {
    return description.getName();
  }

  @Override
  public boolean isApplicable(Procedure info) {
    boolean result = true;
    result &= this.getName().equals(info.getName());
    result &= this.argClass.getName().equals(info.getArgumentType());
    result &= this.resultClass.getName().equals(info.getResultType());
    return result;
  }

  @Override
  public boolean isApplicable(NetworkProtocol.ProcedureDescription procedure) {
    boolean result = true;
    result &= this.getName().equals(procedure.getName());
    result &= this.argClass.getName().equals(procedure.getArgumentType());
    result &= this.resultClass.getName().equals(procedure.getResultType());
    return result;
  }

  protected final void validate(NetworkProtocol.ProcedureDescription procedure) throws IllegalArgumentException {
    Preconditions.checkArgument(this.getName().equals(procedure.getName()));
    Preconditions.checkArgument(this.argClass.getName().equals(procedure.getArgumentType()));
    Preconditions.checkArgument(this.resultClass.getName().equals(procedure.getResultType()));
  }

  @Override
  public int compareTo(CallableProcedure<?, ?> o) {
    return description.compareTo(o.getDescription());
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

  @Override
  public ProcedureCallResult<T, R> call(Node node, T argument) {
    return network.getProcedureManager().callProcedure(node, this, argument);
  }

  @Override
  public ProcedureCallResult<T, R> call(Node node, T argument, long timeout) {
    return network.getProcedureManager().callProcedure(node, this, argument, timeout);
  }

  @Override
  public Map<UUID, ? extends ListenableFuture<R>> call(Collection<Node> nodes, T argument) {
    return network.getProcedureManager().callProcedure(nodes, this, argument);
  }

  @Override
  public Map<UUID, ? extends ListenableFuture<R>> call(Collection<Node> nodes, T argument, long timeout) {
    return network.getProcedureManager().callProcedure(nodes, this, argument, timeout);
  }


}
