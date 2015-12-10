package de.rennschnitzel.backbone.api.network.target;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import de.rennschnitzel.backbone.ProtocolUtils;
import de.rennschnitzel.backbone.api.network.Network;
import de.rennschnitzel.backbone.api.network.Server;
import de.rennschnitzel.backbone.net.protocol.ComponentUUID;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;
import lombok.RequiredArgsConstructor;

public class Target implements TargetOrBuilder {

  // *************************************************************************
  // Static helper

  public static boolean serverInTarget(TransportProtocol.TargetMessageOrBuilder target, Server server) {
    if (target.getAll()) {
      return true;
    }
    for (String namespace : target.getNamespacesList()) {
      if (server.getNamespaces().contains(namespace)) {
        return true;
      }
    }
    ComponentUUID.UUID id = ProtocolUtils.convert(server.getID());
    if (target.getServersList().contains(id)) {
      return true;
    }
    return false;
  }

  public static Target toAll() {
    return new Builder().toAll();
  }

  public static Builder toServer(Server server, Server... servers) {
    return new Builder().add(server).add(servers);
  }


  public static Builder toServer(Collection<Server> servers) {
    Preconditions.checkArgument(!servers.isEmpty());
    return new Builder().add(servers);
  }

  public static Builder toServer(UUID uuid, UUID... uuids) {
    return new Builder().addUUID(uuid).addUUIDs(uuids);
  }

  public static Builder toNamespace(String namespace, String... namespaces) {
    return new Builder().addNamespace(namespace).addNamespaces(namespaces);
  }

  // *************************************************************************
  // Target instance

  private final TransportProtocol.TargetMessage value;

  public Target(TransportProtocol.TargetMessage value) {
    Preconditions.checkNotNull(value);
    Preconditions.checkArgument(value.getAll() || value.getNamespacesCount() > 0 || value.getServersCount() > 0);
    this.value = value;
  }

  @Override
  public TransportProtocol.TargetMessage toProtocol() {
    return value;
  }

  public boolean contains(Server server) {
    return serverInTarget(value, server);
  }


  public void send(String key, byte[] data) {
    Network.getInstance().sendBytes(this, key, data);
  }

  public void send(Object object) {
    Network.getInstance().sendObject(this, object);
  }

  public boolean isToAll() {
    return this.value.getAll();
  }

  public Set<UUID> getServers() {
    return this.value.getServersList().stream().map(ProtocolUtils::convert).collect(Collectors.toSet());
  }

  public Set<String> getNamespaces() {
    return Sets.newHashSet(this.value.getNamespacesList());
  }

  public boolean isEmpty() {
    return !isToAll() && this.value.getServersCount() == 0 && this.value.getNamespacesCount() == 0;
  }

  public Map<UUID, Server> asMap() {
    return Network.getInstance().getServers(this);
  }

  public Target build() {
    return this;
  }


  // *************************************************************************
  // Builder

  @RequiredArgsConstructor
  public static class Builder implements TargetOrBuilder {

    private final TransportProtocol.TargetMessage.Builder b = TransportProtocol.TargetMessage.newBuilder();

    public Target build() {
      return new Target(b.build());
    }

    public TransportProtocol.TargetMessage toProtocol() {
      return b.build();
    }

    public Target toAll() {
      b.setAll(true);
      return build();
    }

    public Builder add(Server server) {
      return addUUID(server.getID());
    }

    public Builder add(Server... servers) {
      for (int i = 0; i < servers.length; ++i) {
        add(servers[i]);
      }
      return this;
    }

    public Builder addUUID(UUID server) {
      b.addServers(ProtocolUtils.convert(server));
      return this;
    }


    public Builder addUUIDs(UUID... servers) {
      for (int i = 0; i < servers.length; ++i) {
        addUUID(servers[i]);
      }
      return this;
    }

    public Builder add(Iterable<Server> servers) {
      servers.forEach(this::add);
      return this;
    }


    public Builder addUUIDs(Iterable<UUID> servers) {
      servers.forEach(this::addUUID);
      return this;
    }

    public Builder addNamespace(String namespace) {
      b.addNamespaces(namespace.toLowerCase());
      return this;
    }

    public Builder addNamespaces(Iterable<String> namespaces) {
      namespaces.forEach(this::addNamespace);
      return this;
    }

    public Builder addNamespaces(String... namespaces) {
      for (int i = 0; i < namespaces.length; ++i) {
        addNamespace(namespaces[i]);
      }
      return this;
    }

    @Override
    public void send(String key, byte[] data) {
      build().send(key, data);
    }

    @Override
    public void send(Object object) {
      build().send(object);
    }

    @Override
    public boolean contains(Server server) {
      return serverInTarget(b, server);
    }


  }


}
