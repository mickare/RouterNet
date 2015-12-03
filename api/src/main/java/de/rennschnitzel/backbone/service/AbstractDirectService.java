package de.rennschnitzel.backbone.service;

import com.google.common.util.concurrent.AbstractService;

public abstract class AbstractDirectService extends AbstractService {

  protected abstract void onStart() throws Exception;

  protected abstract void onStop() throws Exception;

  @Override
  protected void doStart() {
    try {
      onStart();
      notifyStarted();
    } catch (Throwable t) {
      notifyFailed(t);
    }
  }

  @Override
  protected void doStop() {
    try {
      onStop();
      notifyStopped();
    } catch (Throwable t) {
      notifyFailed(t);
    }
  }

}
