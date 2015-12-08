package de.rennschnitzel.backbone.api.network.procedure;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractFuture;

import de.rennschnitzel.backbone.net.protocol.TransportProtocol;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureResponse;
import de.rennschnitzel.backbone.netty.exception.ConnectionException;
import lombok.Getter;

public class ProcedureCall<T, R> extends AbstractFuture<R> {

  private static final AtomicInteger ID_COUNTER = new AtomicInteger(new Random().nextInt());

  @Getter
  private final int id = ID_COUNTER.incrementAndGet();
  private final long timestamp = System.currentTimeMillis();

  private final Procedure<T, R> procedure;
  private final T arg;

  public ProcedureCall(Procedure<T, R> procedure, T arg) {
    Preconditions.checkNotNull(procedure);
    this.procedure = procedure;
    this.arg = arg;
  }

  public void receive(ProcedureResponse response) throws IllegalArgumentException {
    if (!procedure.isApplicable(response.getProcedure())) {
      throw new IllegalArgumentException("response is not applicable for procedure");
    }
    if(response.getCancelled()) {
      super.cancel(true);
    }
    if (response.getDataCase() == ProcedureResponse.DataCase.ERROR) {
      this.setException(new ConnectionException(response.getError()));
    } else {
      try {
        procedure.getResponseReader().apply(response);
      } catch (Exception e) {
        this.setException(new ConnectionException(response.getError()));
      }
    }
  }

  public TransportProtocol.ProcedureCall toProtocol() {
    TransportProtocol.ProcedureCall.Builder b = TransportProtocol.ProcedureCall.newBuilder();
    b.setProcedure(this.procedure.toProtocol());
    b.setId(id);
    b.setTimestamp(timestamp);
    procedure.getCallWriter().accept(b, arg);
    return b.build();
  }


}
