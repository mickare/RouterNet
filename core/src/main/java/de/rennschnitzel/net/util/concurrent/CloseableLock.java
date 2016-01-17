package de.rennschnitzel.net.util.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;

/**
 * 
 * @author mickare
 *
 */
public interface CloseableLock extends Lock, AutoCloseable {

  public CloseableLock open();

  public void close();

  CloseableLock open(long time, TimeUnit unit) throws TimeoutException, InterruptedException;

}
