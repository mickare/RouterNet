package de.rennschnitzel.net.core.procedure;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.rennschnitzel.net.core.Target;
import de.rennschnitzel.net.protocol.TransportProtocol;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureResponseMessage;

public interface ProcedureCall<T, R> {

  int getId();

  long getTimestamp();

  long getMaxTimeout();

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

  void await() throws InterruptedException;

  void await(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException;

  boolean setException(Throwable throwable);

  boolean setException(UUID receiver, Throwable throwable);

  void execute(CallableRegisteredProcedure<T, R> procedure) throws IllegalArgumentException;


}
