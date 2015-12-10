package de.rennschnitzel.backbone.api.network.procedure;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.api.network.Server;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ContentMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureResponseMessage;
import de.rennschnitzel.backbone.netty.exception.ConnectionException;
import lombok.Getter;

public class SingleProcedureCall<T, R> extends AbstractProcedureCall<T, R> {

  @Getter
  private final Server server;
  @Getter
  private final ProcedureCallResult<T, R> result;

  public SingleProcedureCall(Server server, Procedure<T, R> procedure, T argument, long maxTimeout) {
    super(procedure, argument, maxTimeout);
    Preconditions.checkNotNull(server);
    this.server = server;
    this.result = new ProcedureCallResult<T, R>(this, server);

    if (!server.hasProcedure(procedure.getInfo())) {
      setException(new UndefinedServerProcedure());
    }

  }

  public void receive(ContentMessage message, ProcedureResponseMessage response) throws IllegalArgumentException {
    if (!getProcedure().isApplicable(response.getProcedure())) {
      throw new IllegalArgumentException("response is not applicable for procedure");
    }
    UUID senderId = new UUID(message.getSender().getMostSignificantBits(), message.getSender().getLeastSignificantBits());
    Preconditions.checkArgument(senderId.equals(server.getID()), "Wrong response sender");
    if (response.getCancelled()) {
      result.cancel(true);
      return;
    }
    if (response.getDataCase() == ProcedureResponseMessage.DataCase.ERROR) {
      result.setException(new ConnectionException(response.getError()));
    } else {
      try {
        result.set(getProcedure().getResponseReader().apply(response));
      } catch (Exception e) {
        result.setException(new ConnectionException(response.getError()));
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
