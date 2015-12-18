package de.rennschnitzel.backbone.net;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.nustaq.serialization.FSTConfiguration;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import de.rennschnitzel.backbone.net.node.HomeNode;
import de.rennschnitzel.backbone.net.node.NetworkNode;
import de.rennschnitzel.backbone.net.procedure.ProcedureCall;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

public abstract class Network {

  public static FSTConfiguration SERIALIZATION = FSTConfiguration.createDefaultConfiguration();

  @Getter
  @Setter(AccessLevel.PROTECTED)
  private static Network instance = null;

  private final ConcurrentMap<UUID, NetworkNode> servers = new ConcurrentHashMap<>();

  @Getter
  private final HomeNode home;

  public Network(HomeNode home) {
    Preconditions.checkNotNull(home);
    this.home = home;
    this.servers.putIfAbsent(home.getId(), home);
  }

  public Map<UUID, NetworkNode> getServers() {
    return Collections.unmodifiableMap(servers);
  }

  public Map<UUID, NetworkNode> getServersOfTarget(Target target) {
    ImmutableMap.Builder<UUID, NetworkNode> b = ImmutableMap.builder();
    this.servers.values().stream().filter(target::contains).forEach(n -> b.put(n.getId(), n));
    return b.build();
  }

  public Map<UUID, NetworkNode> getServersOfNamespace(String namespace, String... namespaces) {
    Set<String> setN = Sets.newHashSet(namespaces);
    setN.add(namespace);
    setN.remove(null);
    return getServersOfNamespace(setN);
  }

  public Map<UUID, NetworkNode> getServersOfNamespace(Collection<String> namespaces) {
    Preconditions.checkNotNull(namespaces);
    ImmutableMap.Builder<UUID, NetworkNode> b = ImmutableMap.builder();
    this.servers.values().stream().filter(s -> s.hasNamespace(namespaces));
    return b.build();
  }
  
  public abstract <T, R> void sendCall(ProcedureCall<T, R> call);

  public abstract ProcedureManager getProcedureManager();

  public void publishChanges(HomeNode homeNode) {
    // TODO Auto-generated method stub

  }


}
