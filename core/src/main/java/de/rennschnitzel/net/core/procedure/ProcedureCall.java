package de.rennschnitzel.net.core.procedure;

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

  T getArgument();

  void receive(ProcedureMessage message, ProcedureResponseMessage response) throws IllegalArgumentException;

  TransportProtocol.ProcedureCallMessage toProtocol();

  void checkTimeout();

  boolean isDone();

  void await() throws InterruptedException;

  void await(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException;

  boolean setException(Throwable throwable);

}
