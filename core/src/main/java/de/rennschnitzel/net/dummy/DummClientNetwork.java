package de.rennschnitzel.net.dummy;

import java.util.UUID;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.AbstractClientNetwork;
import de.rennschnitzel.net.core.Node.HomeNode;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeMessage.Type;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;
import lombok.Getter;

public class DummClientNetwork extends AbstractClientNetwork {

  private static Logger LOGGER_DEFAULT = new DummyLogger("DummyNetwork", System.out);

  @Getter
  private final EventExecutorGroup executor;

  @Getter
  private Logger logger = LOGGER_DEFAULT;

  public DummClientNetwork() {
    this(new DefaultEventExecutor());
  }

  public DummClientNetwork(UUID uuid) {
    this(new HomeNode(uuid));
  }

  public DummClientNetwork(HomeNode home) {
    this(new DefaultEventExecutor(), home);
  }

  public DummClientNetwork(EventExecutorGroup executor, UUID uuid) {
    this(executor, new HomeNode(uuid));
  }

  public DummClientNetwork(EventExecutorGroup executor) {
    this(executor, new HomeNode(UUID.randomUUID()));
  }

  public DummClientNetwork(EventExecutorGroup executor, HomeNode home) {
    super(home);
    Preconditions.checkNotNull(executor);
    this.executor = executor;
    home.setType(Type.BUKKIT);
  }

  public void setName(String name) {
    this.logger = new DummyLogger(name, System.out);
  }

  public UUID newNotUsedUUID() {
    UUID result;
    do {
      result = UUID.randomUUID();
    } while (this.getNode(result) != null);
    return result;
  }


}
