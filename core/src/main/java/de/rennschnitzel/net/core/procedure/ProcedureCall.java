package de.rennschnitzel.net.core.procedure;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import de.rennschnitzel.net.core.Target;
import de.rennschnitzel.net.protocol.TransportProtocol;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureResponseMessage;

public interface ProcedureCall<T, R> {

  int getId();

  long getTimestamp();

  long getMaxTimeout();

  /**
   * Gets the milliseconds until the timeout of this call is reached.
   * 
   * @return negative timeout if the timeout has been reached.
   */
  default long getRemainingTimeout() {
    return getTimestamp() + getMaxTimeout() - System.currentTimeMillis();
  }

  CallableProcedure<T, R> getProcedure();

  Target getTarget();

  Set<UUID> getNodeUUIDs();

  T getArgument();

  void receive(ProcedureMessage message, ProcedureResponseMessage response) throws IllegalArgumentException;

  TransportProtocol.ProcedureCallMessage toProtocol();

  /**
   * Checks the call for timeout
   * 
   * @return true if timeout
   */
  boolean checkTimeout();

  boolean isDone();

  ProcedureCall<T, R> await() throws InterruptedException;

  ProcedureCall<T, R> await(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException;

  boolean setException(Throwable throwable);

  boolean setException(UUID receiver, Throwable throwable);

  void execute(CallableRegisteredProcedure<T, R> procedure) throws IllegalArgumentException;

  void cancel();

  ProcedureCall<T, R> addListener(Consumer<Collection<ProcedureCallResult<T, R>>> listener);

  ProcedureCall<T, R> addListenerEach(Consumer<ProcedureCallResult<T, R>> listener);

}
