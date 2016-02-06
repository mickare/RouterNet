package de.rennschnitzel.net.core.procedure;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import de.rennschnitzel.net.ProtocolUtils;
import de.rennschnitzel.net.core.Node;
import de.rennschnitzel.net.core.Target;
import de.rennschnitzel.net.exception.ConnectionException;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureResponseMessage;
import lombok.Getter;

public class SingleProcedureCall<T, R> extends AbstractProcedureCall<T, R> {

  private @Getter final Node node;
  private @Getter final ProcedureCallResult<T, R> result;

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
  public SingleProcedureCall<T, R> await() throws InterruptedException {
    try {
      this.result.get();
    } catch (ExecutionException e) {
      // Ignore it! We only await the done state.
    }
    return this;
  }

  @Override
  public SingleProcedureCall<T, R> await(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
    try {
      this.result.get(timeout, unit);
    } catch (ExecutionException e) {
      // Ignore it! We only await the done state.
    }
    return this;
  }

  @Override
  public boolean setException(Throwable throwable) {
    return this.result.setException(throwable);
  }

  @Override
  public boolean setException(UUID receiver, Throwable throwable) {
    if (this.node.getId().equals(receiver)) {
      return this.setException(throwable);
    }
    return false;
  }

  @Override
  public Set<UUID> getNodeUUIDs() {
    return Sets.newHashSet(this.node.getId());
  }


  @Override
  public SingleProcedureCall<T, R> addListener(final Consumer<Collection<ProcedureCallResult<T, R>>> listener) {
    this.result.addListener(f -> listener.accept(Arrays.asList(this.result)));
    return this;
  }

  @Override
  public SingleProcedureCall<T, R> addListenerEach(final Consumer<ProcedureCallResult<T, R>> listener) {
    this.result.addResultListener(listener);
    return this;
  }


}
