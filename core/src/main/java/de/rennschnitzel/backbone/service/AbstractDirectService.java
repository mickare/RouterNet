package de.rennschnitzel.backbone.service;

import com.google.common.util.concurrent.AbstractService;

public abstract class AbstractDirectService extends AbstractService {

  protected abstract void startUp() throws Exception;

  protected abstract void shutDown() throws Exception;

  @Override
  protected void doStart() {
    try {
      startUp();
      notifyStarted();
    } catch (Throwable t) {
      notifyFailed(t);
    }
  }

  @Override
  protected void doStop() {
    try {
      shutDown();
      notifyStopped();
    } catch (Throwable t) {
      notifyFailed(t);
    }
  }

}
