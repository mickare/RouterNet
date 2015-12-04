package de.rennschnitzel.backbone.api.network.procedure;

import java.util.Objects;
import java.util.function.BiConsumer;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.net.protocol.NetworkProtocol;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureCall;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureResponse;
import de.rennschnitzel.backbone.util.CheckedFunction;
import lombok.Getter;

@Getter
public abstract class AbstractProcedure<T, R> implements Procedure<T, R> {

  private final String name;
  private final Class<T> argClass;
  private final Class<R> resultClass;
  private final CheckedFunction<ProcedureCall, T> callReader;
  private final BiConsumer<ProcedureCall.Builder, T> callWriter;
  private final CheckedFunction<ProcedureResponse, R> responseReader;
  private final BiConsumer<ProcedureResponse.Builder, R> responseWriter;


  public AbstractProcedure(final String name, final Class<T> argClass, final Class<R> resultClass) {
    Preconditions.checkArgument(!name.isEmpty());
    Preconditions.checkNotNull(argClass);
    Preconditions.checkNotNull(resultClass);
    this.name = name;
    this.argClass = argClass;
    this.resultClass = resultClass;

    // Compile reader / writer
    this.callReader = ProcedureUtils.compileCallReader(argClass);
    this.callWriter = ProcedureUtils.compileCallWriter(argClass);
    this.responseReader = ProcedureUtils.compileResponseReader(resultClass);
    this.responseWriter = ProcedureUtils.compileResponseWriter(resultClass);

  }

  @Override
  public String getArgClassName() {
    return getArgClass().getName();
  }

  @Override
  public String getResultClassName() {
    return getResultClass().getName();
  }

  protected void validate(NetworkProtocol.Procedure procedure) throws IllegalArgumentException {
    Preconditions.checkArgument(this.name.equals(procedure.getName()));
    Preconditions.checkArgument(this.argClass.getName().equals(procedure.getArgument()));
    Preconditions.checkArgument(this.resultClass.getName().equals(procedure.getResult()));
  }

  public NetworkProtocol.Procedure toProtocol() {
    NetworkProtocol.Procedure.Builder b = NetworkProtocol.Procedure.newBuilder();
    b.setName(name);
    b.setArgument(getArgClassName());
    b.setResult(getResultClassName());
    return b.build();
  }

  @Override
  public int compareTo(Procedure<?, ?> o) {
    if (this == o) {
      return 0;
    }
    int n = String.CASE_INSENSITIVE_ORDER.compare(name, o.getName());
    if (n != 0) {
      return n;
    }
    int arg = String.CASE_INSENSITIVE_ORDER.compare(getArgClassName(), o.getArgClassName());
    if (arg != 0) {
      return arg;
    }
    return String.CASE_INSENSITIVE_ORDER.compare(getResultClassName(), o.getResultClassName());
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof Procedure)) {
      return false;
    }
    Procedure<?, ?> o = (Procedure<?, ?>) object;
    return compareTo(o) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, argClass, resultClass);
  }


}
