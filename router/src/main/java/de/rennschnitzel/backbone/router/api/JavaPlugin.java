package de.rennschnitzel.backbone.router.api;

import java.util.concurrent.Executor;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.MoreExecutors;

import lombok.Getter;
import de.rennschnitzel.backbone.router.Router;

public abstract class JavaPlugin extends AbstractIdleService {

  @Getter
  private final Router niflhel;

  @Getter
  private final String name;

  @Getter
  private final Logger logger;

  public JavaPlugin(Router niflhel, String name) {
    Preconditions.checkNotNull(niflhel);
    Preconditions.checkNotNull(name);
    this.niflhel = niflhel;
    this.name = name;

    this.logger = new PluginLogger(this);
  }

  @Override
  protected Executor executor() {
    return MoreExecutors.directExecutor();
  }

}
