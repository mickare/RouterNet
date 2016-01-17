package de.rennschnitzel.net;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import com.google.common.eventbus.EventBus;

import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.Namespace;
import de.rennschnitzel.net.core.Node;
import de.rennschnitzel.net.core.ProcedureManager;
import de.rennschnitzel.net.core.Target;
import de.rennschnitzel.net.core.Node.HomeNode;
import de.rennschnitzel.net.core.channel.Channel;
import de.rennschnitzel.net.exception.NotConnectedException;
import lombok.Getter;

public class Backbone {

  @Getter
  private static Network network = null;

  public synchronized void setNetwork(Network network) {
    if (Backbone.network != null) {
      throw new UnsupportedOperationException("Cannot redefine singleton Network");
    }
    Backbone.network = network;
  }

  public static HomeNode getHome() {
    return network.getHome();
  }

  public static ProcedureManager getProcedureManager() {
    return network.getProcedureManager();
  }

  public static EventBus getEventBus() {
    return network.getEventBus();
  }

  public static Logger getLogger() {
    return network.getLogger();
  }

  // ***************************************************************************
  // Connection

  public static Connection getConnection() throws NotConnectedException {
    return network.getConnection();
  }

  public static Channel getChannel(String name) throws NotConnectedException {
    return getConnection().getChannel(name);
  }

  public static Channel getChannelIfPresent(String name) throws NotConnectedException {
    return getConnection().getChannelIfPresent(name);
  }


  // ***************************************************************************
  // Nodes

  public static Set<Node> getNodes() {
    return network.getNodes();
  }

  public static Set<Node> getNodes(Target target) {
    return network.getNodes(target);
  }

  public static Node getNode(UUID id) {
    return network.getNode(id);
  }

  public static Node getNode(String name) {
    return network.getNode(name);
  }

  public static Set<Node> getNodes(Namespace namespace) {
    return network.getNodes(namespace);
  }

  public static Set<Node> getNodes(Namespace namespace, Namespace... namespaces) {
    return network.getNodes(namespace, namespaces);
  }

  public static Set<Node> getNodes(Collection<Namespace> namespaces) {
    return network.getNodes(namespaces);
  }

  public static Set<Node> getNodesOfNamespace(String namespace, String... namespaces) {
    return network.getNodesOfNamespace(namespace, namespaces);
  }

  public static Namespace getNamespace(String namespace) {
    return network.getNamespace(namespace);
  }

}
