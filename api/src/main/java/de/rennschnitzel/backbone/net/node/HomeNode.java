package de.rennschnitzel.backbone.net.node;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import de.rennschnitzel.backbone.net.Network;
import de.rennschnitzel.backbone.net.procedure.RegisteredProcedure;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.ServerMessage;
import lombok.Getter;

public class HomeNode extends BaseNetworkNode {

  @Getter
  private volatile boolean dirty = false;

  public HomeNode(UUID id) {
    super(id);
  }

  public HomeNode(UUID id, Collection<String> namespaces) {
    super(id);
    Set<String> temp = Sets.newHashSet();
    namespaces.stream().filter(n -> !n.isEmpty()).forEach(n -> temp.add(n.toLowerCase()));
    this.namespaces.addAll(temp);
  }

  public void addNamespace(String namespace) {
    Preconditions.checkArgument(!namespace.isEmpty());
    dirty &= this.namespaces.add(namespace.toLowerCase());
  }

  public void removeNamespace(String namespace) {
    Preconditions.checkArgument(!namespace.isEmpty());
    dirty &= this.namespaces.remove(namespace.toLowerCase());
  }

  public void setName(String name) {
    if (name == null) {
      this.name = Optional.empty();
    } else {
      this.name = Optional.ofNullable(name.isEmpty() ? null : name.toLowerCase());
    }
    dirty = true;
  }

  public void addRegisteredProcedure(RegisteredProcedure<?, ?> procedure) {
    dirty &= this.procedures.add(procedure.getInfo());
  }

  public void removeRegisteredProcedure(RegisteredProcedure<?, ?> procedure) {
    dirty &= this.procedures.remove(procedure.getInfo());
  }

  public void publishChanges() {
    if (!this.dirty) {
      return;
    }
    synchronized (this) {
      if (!this.dirty) {
        return;
      }
      this.dirty = false;
      Network.getInstance().publishChanges(this);
    }
  }

  @Override
  public void update(ServerMessage server) {
    throw new UnsupportedOperationException("home server cant be updated via network");
  }

}
