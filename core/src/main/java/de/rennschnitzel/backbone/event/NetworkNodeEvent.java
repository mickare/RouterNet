package de.rennschnitzel.backbone.event;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.net.Network;
import de.rennschnitzel.backbone.net.Node;
import lombok.Getter;
import lombok.NonNull;

public class NetworkNodeEvent extends NetworkEvent {


  @Getter
  @NonNull
  private final Node node;


  public NetworkNodeEvent(Network network, Node node) {
    super(network);
    // TODO Auto-generated constructor stub
    Preconditions.checkNotNull(node);
    this.node = node;
  }


  public static class NetworkNodeAddedEvent extends NetworkNodeEvent {
    public NetworkNodeAddedEvent(Network network, Node node) {
      super(network, node);
    }
  }

  public static class NetworkNodeUpdatedEvent extends NetworkNodeEvent {
    public NetworkNodeUpdatedEvent(Network network, Node node) {
      super(network, node);
    }
  }

  public static class NetworkNodeRemovedEvent extends NetworkNodeEvent {
    public NetworkNodeRemovedEvent(Network network, Node node) {
      super(network, node);
    }
  }


}
