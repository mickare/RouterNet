package de.rennschnitzel.net.event;

import java.util.Optional;
import java.util.UUID;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeTopologyMessage;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class LoginSuccessEvent extends NetworkEvent {

  @NonNull
  private final UUID id;
  @NonNull
  private final Optional<String> name;


  public LoginSuccessEvent(AbstractNetwork network, UUID id, Optional<String> name) {
    super(network);
    Preconditions.checkNotNull(id);
    Preconditions.checkNotNull(name);
    this.id = id;
    this.name = name;
  }

  public LoginSuccessEvent(AbstractNetwork network, UUID id, String name) {
    this(network, id, Optional.<String>ofNullable(name));
  }

  @Getter
  public static class ClientLoginSuccessEvent extends LoginSuccessEvent {

    private final NodeMessage nodeMessage;

    public ClientLoginSuccessEvent(AbstractNetwork network, UUID id, String name, NodeMessage nodeMessage) {
      this(network, id, Optional.ofNullable(name), nodeMessage);
    }

    public ClientLoginSuccessEvent(AbstractNetwork network, UUID id, Optional<String> name, NodeMessage nodeMessage) {
      super(network, id, name);
      this.nodeMessage = nodeMessage;
    }

    @Override
    public String toString() {
      String name = this.getName().isPresent() ? (", " + this.getName().get()) : "";
      return "ClientLoginSuccessEvent(" + this.getId() + name + ")\n" + nodeMessage.toString();
    }

  }

  @Getter
  public static class RouterLoginSuccessEvent extends LoginSuccessEvent {

    private final NodeTopologyMessage nodeTopology;

    public RouterLoginSuccessEvent(AbstractNetwork network, UUID id, String name, NodeTopologyMessage nodeTopology) {
      this(network, id, Optional.ofNullable(name), nodeTopology);
    }

    public RouterLoginSuccessEvent(AbstractNetwork network, UUID id, Optional<String> name, NodeTopologyMessage nodeTopology) {
      super(network, id, name);
      this.nodeTopology = nodeTopology;
    }

    @Override
    public String toString() {
      String name = this.getName().isPresent() ? (", " + this.getName().get()) : "";
      return "RouterLoginSuccessEvent(" + this.getId() + name + ")\n" + nodeTopology.toString();
    }

  }


}
