package de.rennschnitzel.backbone.event;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.net.Network;
import de.rennschnitzel.backbone.net.node.NetworkNode;
import lombok.Getter;
import lombok.NonNull;

public class ServerNetworkEvent extends NetworkEvent {


  @Getter
  @NonNull
  private final NetworkNode server;


  public ServerNetworkEvent(Network network, NetworkNode server) {
    super(network);
    // TODO Auto-generated constructor stub
    Preconditions.checkNotNull(server);
    this.server = server;
  }


  public static class AddedServerNetworkEvent extends ServerNetworkEvent {
    public AddedServerNetworkEvent(Network network, NetworkNode server) {
      super(network, server);
    }
  }


  public static class RemovedServerNetworkEvent extends ServerNetworkEvent {
    public RemovedServerNetworkEvent(Network network, NetworkNode server) {
      super(network, server);
    }
  }


}
