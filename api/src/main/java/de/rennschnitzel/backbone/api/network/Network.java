package de.rennschnitzel.backbone.api.network;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.nustaq.serialization.FSTConfiguration;

import de.rennschnitzel.backbone.api.network.procedure.ProcedureCall;
import de.rennschnitzel.backbone.api.network.target.TargetOrBuilder;
import de.rennschnitzel.backbone.net.node.HomeNode;
import de.rennschnitzel.backbone.net.node.NetworkNode;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;
import de.rennschnitzel.backbone.netty.exception.ProtocolException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

public abstract class Network {

  public static FSTConfiguration FST = FSTConfiguration.createDefaultConfiguration();

  @Getter
  @Setter(AccessLevel.PROTECTED)
  private static Network instance = null;

  public abstract HomeNode getHome();

  public abstract Map<UUID, NetworkNode> getServers();

  public abstract Map<UUID, NetworkNode> getServersOfTarget(TargetOrBuilder target);

  public abstract Map<UUID, NetworkNode> getServersOfNamespace(String namespace, String... namespaces);

  public abstract <T, R> void sendCall(ProcedureCall<T, R> call);

  public void publishChanges(HomeNode homeNode) {
    // TODO Auto-generated method stub

  }


}
