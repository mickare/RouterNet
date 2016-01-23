package de.rennschnitzel.net.dummy;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.AbstractClientNetwork;
import de.rennschnitzel.net.core.Node.HomeNode;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeMessage.Type;
import de.rennschnitzel.net.util.concurrent.DirectScheduledExecutorService;
import lombok.Getter;

public class DummClientNetwork extends AbstractClientNetwork {

  private static Logger LOGGER_DEFAULT = new DummyLogger("DummyNetwork", System.out);

  @Getter
  private final ScheduledExecutorService executor;

  @Getter
  private Logger logger = LOGGER_DEFAULT;

  public DummClientNetwork() {
    this(new DirectScheduledExecutorService());
  }

  public DummClientNetwork(UUID uuid) {
    this(new HomeNode(uuid));
  }

  public DummClientNetwork(HomeNode home) {
    this(new DirectScheduledExecutorService(), home);
  }

  public DummClientNetwork(ScheduledExecutorService executor) {
    this(executor, new HomeNode(UUID.randomUUID()));
  }

  public DummClientNetwork(ScheduledExecutorService executor, HomeNode home) {
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
