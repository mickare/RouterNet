package de.rennschnitzel.backbone.net.procedure;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractFuture;

import de.rennschnitzel.backbone.net.Node;
import lombok.Getter;

public class ProcedureCallResult<T, R> extends AbstractFuture<R> {

  @Getter
  private final ProcedureCall<T, R> call;
  @Getter
  private final Node node;
  @Getter
  private long completionTime = -1;

  protected ProcedureCallResult(ProcedureCall<T, R> call, Node node) {
    Preconditions.checkNotNull(call);
    Preconditions.checkNotNull(node);
    this.call = call;
    this.node = node;
  }

  private boolean setCompletionTime(boolean doSet) {
    if (doSet) {
      completionTime = System.currentTimeMillis();
    }
    return doSet;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return setCompletionTime(super.cancel(mayInterruptIfRunning));
  }

  @Override
  protected boolean set(R value) {
    return setCompletionTime(super.set(value));
  }

  @Override
  protected boolean setException(Throwable throwable) {
    return setCompletionTime(super.setException(throwable));
  }

}
