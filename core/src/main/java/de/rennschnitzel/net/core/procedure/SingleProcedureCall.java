package de.rennschnitzel.net.core.procedure;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.ProtocolUtils;
import de.rennschnitzel.net.core.Node;
import de.rennschnitzel.net.core.Target;
import de.rennschnitzel.net.exception.ConnectionException;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureResponseMessage;
import lombok.Getter;

public class SingleProcedureCall<T, R> extends AbstractProcedureCall<T, R> {

  @Getter
  private final Node node;
  @Getter
  private final ProcedureCallResult<T, R> result;

  public SingleProcedureCall(Node node, CallableProcedure<T, R> procedure, T argument, long maxTimeout) {
    super(procedure, Target.to(node), argument, maxTimeout);
    Preconditions.checkNotNull(node);
    this.node = node;
    this.result = new ProcedureCallResult<T, R>(this, node);

    if (!node.hasProcedure(procedure)) {
      setException(new UndefinedServerProcedure());
    }

  }

  @Override
  public void execute(CallableRegisteredProcedure<T, R> procedure) throws IllegalArgumentException {
    if (result.isDone()) {
      return;
    }
    if (!getProcedure().isApplicable(procedure)) {
      throw new IllegalArgumentException("response is not applicable for procedure");
    }
    Preconditions.checkArgument(procedure.getNetwork().getHome().getId().equals(node.getId()), "Wrong response sender");
    try {
      this.result.set(procedure.call(this.getArgument()));
    } catch (Exception e) {
      this.result.setException(e);
    }
  }

  @Override
  public void receive(ProcedureMessage message, ProcedureResponseMessage response) throws IllegalArgumentException {
    if (this.isDone()) {
      return;
    }
    if (!getProcedure().isApplicable(response.getProcedure())) {
      throw new IllegalArgumentException("response is not applicable for procedure");
    }
    UUID senderId = ProtocolUtils.convert(message.getSender());
    Preconditions.checkArgument(senderId.equals(node.getId()), "Wrong response sender");
    if (response.getCancelled()) {
      result.cancel(true);
      return;
    }
    if (response.getDataCase() == ProcedureResponseMessage.DataCase.ERROR) {
      result.setException(ConnectionException.of(response.getError()));
    } else {
      try {
        result.set(getProcedure().getResponseReader().apply(response));
      } catch (Exception e) {
        result.setException(ConnectionException.of(response.getError()));
      }
    }
  }

  @Override
  public boolean isDone() {
    return this.result.isDone();
  }


  @Override
  public void await() throws InterruptedException {
    try {
      this.result.get();
    } catch (ExecutionException e) {
      // Ignore it! We only await the done state.
    }
  }

  @Override
  public void await(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
    try {
      this.result.get(timeout, unit);
    } catch (ExecutionException e) {
      // Ignore it! We only await the done state.
    }
  }

  @Override
  public boolean setException(Throwable throwable) {
    return this.result.setException(throwable);
  }

}
