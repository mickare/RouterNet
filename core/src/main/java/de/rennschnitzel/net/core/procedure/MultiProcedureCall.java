package de.rennschnitzel.net.core.procedure;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import de.rennschnitzel.net.core.Node;
import de.rennschnitzel.net.core.Target;
import de.rennschnitzel.net.exception.ConnectionException;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureResponseMessage;
import lombok.Getter;

public class MultiProcedureCall<T, R> extends AbstractProcedureCall<T, R> {

  private final @Getter ImmutableMap<UUID, ProcedureCallResult<T, R>> results;
  private final ListenableFuture<?> future;

  public MultiProcedureCall(Collection<Node> nodes, CallableProcedure<T, R> procedure, T argument, long maxTimeout) {
    super(procedure, Target.to(nodes), argument, maxTimeout);
    Preconditions.checkArgument(!nodes.isEmpty());

    ImmutableMap.Builder<UUID, ProcedureCallResult<T, R>> b = ImmutableMap.builder();
    for (Node node : Sets.newHashSet(nodes)) {

      ProcedureCallResult<T, R> res = new ProcedureCallResult<>(this, node);
      if (!node.hasProcedure(procedure)) {
        res.setException(new UndefinedServerProcedure());
      }

      b.put(node.getId(), res);
    }
    this.results = b.build();
    this.future = Futures.successfulAsList(this.results.values());

  }
  
  @Override
  public void execute(CallableRegisteredProcedure<T, R> procedure) throws IllegalArgumentException {
    if (!getProcedure().isApplicable(procedure)) {
      throw new IllegalArgumentException("response is not applicable for procedure");
    }
    ProcedureCallResult<T, R> future = results.get(procedure.getNetwork().getHome().getId());
    if (future == null) {
      throw new IllegalArgumentException("Wrong response sender");
    }
    if (this.isDone()) {
      return;
    }
    try {
      future.set(procedure.call(this.getArgument()));
    } catch (Exception e) {
      future.setException(e);
    }
  }

  @Override
  public void receive(ProcedureMessage message, ProcedureResponseMessage response) throws IllegalArgumentException {
    if (!getProcedure().isApplicable(response.getProcedure())) {
      throw new IllegalArgumentException("response is not applicable for procedure");
    }
    UUID senderId = new UUID(message.getSender().getMostSignificantBits(), message.getSender().getLeastSignificantBits());
    ProcedureCallResult<T, R> future = results.get(senderId);
    if (future == null) {
      throw new IllegalArgumentException("Wrong response sender");
    }
    if (response.getDataCase() == ProcedureResponseMessage.DataCase.ERROR) {
      future.setException(ConnectionException.of(response.getError()));
    } else {
      try {
        future.set(getProcedure().getResponseReader().apply(response));
      } catch (Exception e) {
        future.setException(ConnectionException.of(response.getError()));
      }
    }
  }

  @Override
  public boolean isDone() {
    return !this.results.values().stream().filter(r -> !r.isDone()).findAny().isPresent();
  }

  @Override
  public MultiProcedureCall<T, R> await() throws InterruptedException {
    try {
      future.get();
    } catch (ExecutionException e) {
      // should not happen;
      throw new RuntimeException(e);
    }
    return this;
  }

  @Override
  public MultiProcedureCall<T, R> await(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
    try {
      future.get(timeout, unit);
    } catch (ExecutionException e) {
      // should not happen;
      throw new RuntimeException(e);
    }
    return this;
  }


  @Override
  public boolean setException(Throwable throwable) {
    boolean changed = false;
    for (ProcedureCallResult<T, R> r : this.results.values()) {
      changed |= r.setException(throwable);
    }
    return changed;
  }

  @Override
  public boolean setException(UUID receiver, Throwable throwable) {
    Preconditions.checkNotNull(receiver);
    boolean changed = false;
    ProcedureCallResult<T, R> r = this.results.get(receiver);
    if (r != null) {
      changed |= r.setException(throwable);
    }
    return changed;
  }

  @Override
  public Set<UUID> getNodeUUIDs() {
    return Collections.unmodifiableSet(this.results.keySet());
  }

  @Override
  public MultiProcedureCall<T, R> addListener(final Consumer<Collection<ProcedureCallResult<T, R>>> listener) {
    return addListener(listener, MoreExecutors.directExecutor());
  }

  @Override
  public MultiProcedureCall<T, R> addListener(final Consumer<Collection<ProcedureCallResult<T, R>>> listener, final Executor executor) {
    this.future.addListener(() -> {
      listener.accept(this.results.values());
    } , executor);
    return this;
  }

  @Override
  public ProcedureCall<T, R> addListenerEach(final Consumer<ProcedureCallResult<T, R>> listener) {
    return addListenerEach(listener, MoreExecutors.directExecutor());
  }

  @Override
  public ProcedureCall<T, R> addListenerEach(final Consumer<ProcedureCallResult<T, R>> listener, final Executor executor) {
    this.results.values().forEach(r -> r.addListener(listener, executor));
    return this;
  }

}
