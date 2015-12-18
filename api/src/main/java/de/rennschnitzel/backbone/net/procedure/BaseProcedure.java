package de.rennschnitzel.backbone.net.procedure;

import java.util.function.BiConsumer;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.net.Network;
import de.rennschnitzel.backbone.net.NetworkMember;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureCallMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureResponseMessage;
import de.rennschnitzel.backbone.util.function.CheckedFunction;
import lombok.Getter;
import lombok.Setter;

@Getter
public class BaseProcedure<T, R> implements Procedure<T, R> {

  private final ProcedureInformation info;
  private final Class<T> argClass;
  private final Class<R> resultClass;
  private final CheckedFunction<ProcedureCallMessage, T> callReader;
  private final BiConsumer<ProcedureCallMessage.Builder, T> callWriter;
  private final CheckedFunction<ProcedureResponseMessage, R> responseReader;
  private final BiConsumer<ProcedureResponseMessage.Builder, R> responseWriter;

  @Getter
  @Setter
  private CheckedFunction<T, R> localFunction = null;

  public BaseProcedure(final ProcedureInformation info, final Class<T> argClass, final Class<R> resultClass) {
    Preconditions.checkNotNull(info);
    Preconditions.checkNotNull(argClass);
    Preconditions.checkNotNull(resultClass);

    this.info = info;
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
    return info.getName();
  }

  @Override
  public boolean isApplicable(NetworkProtocol.ProcedureDescription procedure) {
    boolean result = true;
    result &= this.getName().equals(procedure.getName());
    result &= this.argClass.getName().equals(procedure.getArgument());
    result &= this.resultClass.getName().equals(procedure.getResult());
    return result;
  }

  protected final void validate(NetworkProtocol.ProcedureDescription procedure) throws IllegalArgumentException {
    Preconditions.checkArgument(this.getName().equals(procedure.getName()));
    Preconditions.checkArgument(this.argClass.getName().equals(procedure.getArgument()));
    Preconditions.checkArgument(this.resultClass.getName().equals(procedure.getResult()));
  }

  @Override
  public int compareTo(Procedure<?, ?> o) {
    return info.compareTo(o.getInfo());
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
  public boolean isLocalFunction() {
    return localFunction != null;
  }

  @Override
  public ProcedureCallResult<T, R> call(NetworkMember server, T argument) {
    return Network.getInstance().getProcedureManager().callProcedure(server, this, argument);
  }


}
