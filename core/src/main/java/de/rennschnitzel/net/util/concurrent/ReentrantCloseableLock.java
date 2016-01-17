package de.rennschnitzel.net.util.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 
 * @author mickare
 *
 */
public class ReentrantCloseableLock extends ReentrantLock implements CloseableLock {

  /**
   * 
   */
  private static final long serialVersionUID = -4139432594795219762L;

  @Override
  public ReentrantCloseableLock open() {
    this.lock();
    return this;
  }

  @Override
  public void close() {
    this.unlock();
  }

  @Override
  public ReentrantCloseableLock open(long time, TimeUnit unit) throws TimeoutException, InterruptedException {
    if (!this.tryLock(time, unit)) {
      throw new TimeoutException();
    }
    return this;
  }

}
