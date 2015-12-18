package de.rennschnitzel.backbone.net;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

import com.google.common.base.Preconditions;

import lombok.Getter;

public class NetworkMember {

  @Getter
  private final UUID id;
  @Getter
  private Optional<String> name = Optional.empty();
  private final Set<String> namespaces = new CopyOnWriteArraySet<>();

  public NetworkMember(UUID id) {
    Preconditions.checkNotNull(id);
    this.id = id;
  }

  public Set<String> getNamespaces() {
    return Collections.unmodifiableSet(this.namespaces);
  }

  public boolean hasNamespace(String namespace) {
    return this.namespaces.contains(namespace.toLowerCase());
  }

}
