package de.rennschnitzel.backbone.net;

import java.util.Set;

import com.google.common.base.Preconditions;

import lombok.Getter;

public class Namespace {

  private final AbstractNetwork network;
  @Getter
  private final String name;

  public Namespace(AbstractNetwork network, String name) {
    Preconditions.checkNotNull(network);
    Preconditions.checkArgument(!name.isEmpty());
    this.network = network;
    this.name = name;
  }

  public Set<Node> getNodes() {
    return network.getNodes(this);
  }

}
