package de.rennschnitzel.backbone.net.procedure;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import de.rennschnitzel.backbone.net.Target;
import de.rennschnitzel.backbone.net.node.NetworkNode;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureResponseMessage;
import de.rennschnitzel.backbone.netty.exception.ConnectionException;

public class MultiProcedureCall<T, R> extends AbstractProcedureCall<T, R> {

  private final Map<UUID, ProcedureCallResult<T, R>> results;

  public MultiProcedureCall(Collection<NetworkNode> servers, Procedure<T, R> procedure, T argument, long maxTimeout) {
    super(procedure, Target.to(servers), argument, maxTimeout);
    Preconditions.checkArgument(!servers.isEmpty());

    ImmutableMap.Builder<UUID, ProcedureCallResult<T, R>> b = ImmutableMap.builder();
    for (NetworkNode server : Sets.newHashSet(servers)) {

      ProcedureCallResult<T, R> res = new ProcedureCallResult<>(this, server);
      if (!server.hasProcedure(procedure.getInfo())) {
        res.setException(new UndefinedServerProcedure());
      }

      b.put(server.getId(), res);
    }
    this.results = b.build();

  }

  public Map<UUID, ? extends ListenableFuture<R>> getResult() {
    return this.results;
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
      future.setException(new ConnectionException(response.getError()));
    } else {
      try {
        future.set(getProcedure().getResponseReader().apply(response));
      } catch (Exception e) {
        future.setException(new ConnectionException(response.getError()));
      }
    }
  }

  @Override
  public boolean isDone() {
    return !this.results.values().stream().filter(r -> !r.isDone()).findAny().isPresent();
  }

  @Override
  public void await() throws InterruptedException {
    try {
      Futures.successfulAsList(this.results.values()).get();
    } catch (ExecutionException e) {
      // should not happen;
      throw new RuntimeException(e);
    }
  }

  @Override
  public void await(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
    try {
      Futures.successfulAsList(this.results.values()).get(timeout, unit);
    } catch (ExecutionException e) {
      // should not happen;
      throw new RuntimeException(e);
    }
  }


  @Override
  public boolean setException(Throwable throwable) {
    boolean changed = false;
    for (ProcedureCallResult<T, R> r : this.results.values()) {
      changed |= r.setException(throwable);
    }
    return changed;
  }

}
