package de.rennschnitzel.net.core.procedure;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractFuture;

import de.rennschnitzel.net.core.Node;
import lombok.Getter;

public class ProcedureCallResult<T, R> extends AbstractFuture<R> {

  private @Getter final ProcedureCall<T, R> call;
  private @Getter final Node node;
  private @Getter long completionTime = -1;

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
