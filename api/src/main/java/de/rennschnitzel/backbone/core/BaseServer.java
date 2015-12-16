package de.rennschnitzel.backbone.core;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import de.rennschnitzel.backbone.api.network.Network;
import de.rennschnitzel.backbone.api.network.Server;
import de.rennschnitzel.backbone.api.network.procedure.Procedure;
import de.rennschnitzel.backbone.api.network.procedure.ProcedureCallResult;
import de.rennschnitzel.backbone.api.network.procedure.ProcedureInformation;
import de.rennschnitzel.backbone.api.network.target.Target;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.ServerMessage.Type;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BaseServer implements Server {

  @Getter
  @NonNull
  private final UUID ID;

  @Getter
  @NonNull
  private final Type type;

  @Getter
  private Optional<String> name = Optional.empty();

  @Getter
  private boolean connected = false;

  private final BiMap<ProcedureInformation, Procedure<?, ?>> procedures = Maps.synchronizedBiMap(HashBiMap.create());

  private final Set<String> namespaces = Collections.synchronizedSet(Sets.newHashSet());

  @Override
  public Set<String> getNamespaces() {
    return Collections.unmodifiableSet(this.namespaces);
  }

  @Override
  public BiMap<ProcedureInformation, Procedure<?, ?>> getProcedures() {
    return Maps.unmodifiableBiMap(this.procedures);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T, R> Procedure<T, R> getProcedure(String name, Class<T> argument, Class<R> result) {
    return (Procedure<T, R>) this.procedures.get(new ProcedureInformation(name, argument, result));
  }

  @Override
  public <T, R> ProcedureCallResult<T, R> call(Procedure<T, R> remoteProcedure, T argument) {
    return remoteProcedure.call(this, argument);
  }

  @Override
  public boolean hasNamespace(String namespace) {
    return namespaces.contains(namespace.toLowerCase());
  }

  @Override
  public boolean hasProcedure(ProcedureInformation info) {
    return procedures.containsKey(info);
  }

  @Override
  public void send(String key, byte[] data) {
    Network.getInstance().sendBytes(Target.toServer(this), key, data);
  }

  @Override
  public void send(Object object) {
    Network.getInstance().sendObject(Target.toServer(this), object);
  }

}
