package de.rennschnitzel.backbone.net;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.nustaq.serialization.FSTConfiguration;

import de.rennschnitzel.backbone.net.procedure.ProcedureCall;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

public abstract class Network {

  public static FSTConfiguration FST = FSTConfiguration.createDefaultConfiguration();

  @Getter
  @Setter(AccessLevel.PROTECTED)
  private static Network instance = null;

  public abstract UUID getID();

  public abstract Set<String> getNamespaces();

  public abstract Map<UUID, NetworkMember> getServers();

  public abstract Map<UUID, NetworkMember> getServersOfTarget(Target target);

  public abstract Map<UUID, NetworkMember> getServersOfNamespace(String namespace, String... namespaces);

  public abstract <T, R> void sendCall(ProcedureCall<T, R> call);

  public abstract ProcedureManager getProcedureManager();
  

}
