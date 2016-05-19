package de.rennschnitzel.net;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import com.google.common.eventbus.EventBus;

import de.rennschnitzel.net.core.Tunnel;
import de.rennschnitzel.net.core.Namespace;
import de.rennschnitzel.net.core.Node;
import de.rennschnitzel.net.core.Node.HomeNode;
import de.rennschnitzel.net.core.ProcedureManager;
import de.rennschnitzel.net.core.Target;
import de.rennschnitzel.net.core.tunnel.SubTunnel;
import de.rennschnitzel.net.core.tunnel.SubTunnelDescriptor;
import lombok.Getter;

public class Net {
	
	@Getter
	private static RouterNetwork network = null;
	
	protected static synchronized void setNetwork( RouterNetwork network ) {
		// if (Net.network != null) {
		// throw new UnsupportedOperationException("Cannot redefine singleton Network");
		// }
		Net.network = network;
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
	
	public static Set<Tunnel> getTunnels() {
		return network.getTunnels();
	}
	
	public static Set<SubTunnel> getSubTunnels() {
		return network.getSubTunnels();
	}
	
	public static Tunnel getTunnelIfPresent( String name ) {
		return network.getTunnelIfPresent( name );
	}
	
	public static Tunnel getTunnel( String name ) {
		return network.getTunnel( name );
	}
	
	public static <S extends SubTunnel> S getTunnelIfPresent( SubTunnelDescriptor<S> descriptor ) {
		return network.getTunnelIfPresent( descriptor );
	}
	
	public static <S extends SubTunnel> S getTunnel( SubTunnelDescriptor<S> descriptor ) {
		return network.getTunnel( descriptor );
	}
	
	// ***************************************************************************
	// Nodes
	
	public static Set<Node> getNodes() {
		return network.getNodes();
	}
	
	public static Set<Node> getNodes( Target target ) {
		return network.getNodes( target );
	}
	
	public static Node getNode( UUID id ) {
		return network.getNode( id );
	}
	
	public static Node getNode( String name ) {
		return network.getNode( name );
	}
	
	public static Set<Node> getNodes( Namespace namespace ) {
		return network.getNodes( namespace );
	}
	
	public static Set<Node> getNodes( Namespace namespace, Namespace... namespaces ) {
		return network.getNodes( namespace, namespaces );
	}
	
	public static Set<Node> getNodes( Collection<Namespace> namespaces ) {
		return network.getNodes( namespaces );
	}
	
	public static Set<Node> getNodesOfNamespace( String namespace, String... namespaces ) {
		return network.getNodesOfNamespace( namespace, namespaces );
	}
	
	public static Namespace getNamespace( String namespace ) {
		return network.getNamespace( namespace );
	}
	
}
