package de.rennschnitzel.backbone.api.network.procedure;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractFuture;

import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureCall;
import lombok.Getter;

public class RemoteProcedureCall<T, R> extends AbstractFuture<R> {

  private static final AtomicInteger ID_COUNTER = new AtomicInteger(new Random().nextInt());

  @Getter
  private final int id = ID_COUNTER.incrementAndGet();
  private final long timestamp = System.currentTimeMillis();

  private final Procedure<T, R> procedure;
  private final T arg;

  public RemoteProcedureCall(Procedure<T, R> procedure, T arg) {
    Preconditions.checkNotNull(procedure);
    this.procedure = procedure;
    this.arg = arg;
  }

  public ProcedureCall toProtocol() {
    ProcedureCall.Builder b = ProcedureCall.newBuilder();
    b.setProcedure(this.procedure.toProtocol());
    b.setId(id);
    b.setTimestamp(timestamp);
    procedure.getArgumentWriter().accept(b, arg);
    return b.build();
  }


}
