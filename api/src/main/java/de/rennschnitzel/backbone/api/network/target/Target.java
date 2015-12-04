package de.rennschnitzel.backbone.api.network.target;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.MessageOrBuilder;

import de.rennschnitzel.backbone.ProtocolUtils;
import de.rennschnitzel.backbone.api.network.Network;
import de.rennschnitzel.backbone.api.network.Server;
import de.rennschnitzel.backbone.net.protocol.ComponentUUID;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Target implements TargetOrBuilder {

  public static boolean serverInTarget(TransportProtocol.TargetOrBuilder target, Server server) {
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

  @NonNull
  private final TransportProtocol.Target value;

  @Override
  public TransportProtocol.Target toProtocol() {
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

  public void send(MessageOrBuilder message) {
    Network.getInstance().sendAny(this, message);
  }

  public <T, R> Map<Server, ListenableFuture<T>> callServices(String name, Class<T> argument, Class<R> result) {
    return Network.getInstance().callProcedures(this, name, argument, result);
  }


  @RequiredArgsConstructor
  public static class Builder implements TargetOrBuilder {

    private final TransportProtocol.Target.Builder b = TransportProtocol.Target.newBuilder();

    public Target build() {
      return new Target(b.build());
    }

    public TransportProtocol.Target toProtocol() {
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
    public void send(MessageOrBuilder message) {
      build().send(message);
    }

    @Override
    public <T, R> Map<Server, ListenableFuture<T>> callServices(String name, Class<T> argument, Class<R> result) {
      return build().callServices(name, argument, result);
    }

    @Override
    public boolean contains(Server server) {
      return serverInTarget(b, server);
    }


  }


}
