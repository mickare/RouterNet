package de.rennschnitzel.backbone.api.network.list;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.MessageOrBuilder;

import de.rennschnitzel.backbone.api.network.Server;
import de.rennschnitzel.backbone.api.network.target.MessageReceiver;
import de.rennschnitzel.backbone.api.network.target.Target;
import lombok.Getter;

public class ServerCollection implements Iterable<Server>, MessageReceiver {

  public static ServerCollector SERVER_COLLECTOR = new ServerCollector();

  @Getter
  private final Map<UUID, Server> servers;

  public ServerCollection(Map<UUID, Server> servers) {
    Preconditions.checkNotNull(servers);
    this.servers = Collections.unmodifiableMap(servers);
  }

  public ServerCollection filter(String namespace) {
    return servers.values().stream().filter(s -> s.hasNamespace(namespace)).collect(SERVER_COLLECTOR);
  }

  public Server getServer(UUID uuid) {
    return servers.get(uuid);
  }

  public Server getServer(String name) {
    Optional<Server> o =
        servers.values().stream().filter(s -> s.getName().isPresent() && s.getName().get().equalsIgnoreCase(name)).findAny();
    return o.orElse(null);
  }

  public Set<String> getNamespaces() {
    return servers.values().stream().flatMap(s -> s.getNamespaces().stream()).collect(Collectors.toSet());
  }

  @Override
  public Iterator<Server> iterator() {
    return servers.values().iterator();
  }


  @Override
  public void send(String key, byte[] data) {
    if (servers.isEmpty()) {
      return;
    }
    Target.toServer(servers.values()).send(key, data);
  }

  @Override
  public void send(Object object) {
    if (servers.isEmpty()) {
      return;
    }
    Target.toServer(servers.values()).send(object);
  }

  @Override
  public void send(MessageOrBuilder message) {
    if (servers.isEmpty()) {
      return;
    }
    Target.toServer(servers.values()).send(message);
  }

  @Override
  public <T, R> Map<Server, ListenableFuture<T>> callServices(String name, Class<T> argument, Class<R> result) {
    if (servers.isEmpty()) {
      return Collections.emptyMap();
    }
    return Target.toServer(servers.values()).callServices(name, argument, result);
  }


  public static class ServerCollector implements Collector<Server, Map<UUID, Server>, ServerCollection> {
    @Override
    public Supplier<Map<UUID, Server>> supplier() {
      return Maps::newConcurrentMap;
    }

    @Override
    public BiConsumer<Map<UUID, Server>, Server> accumulator() {
      return (a, t) -> a.put(t.getID(), t);
    }

    @Override
    public BinaryOperator<Map<UUID, Server>> combiner() {
      return (a, b) -> {
        Map<UUID, Server> res = supplier().get();
        res.putAll(a);
        res.putAll(b);
        return res;
      };
    }

    @Override
    public Function<Map<UUID, Server>, ServerCollection> finisher() {
      return ServerCollection::new;
    }

    static ImmutableSet<Characteristics> character = ImmutableSet.of(Characteristics.CONCURRENT, Characteristics.UNORDERED);

    @Override
    public Set<Characteristics> characteristics() {
      return character;
    }
  }



}
