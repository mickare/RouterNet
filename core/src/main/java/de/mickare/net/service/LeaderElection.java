package de.mickare.net.service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;

import de.mickare.net.core.AbstractNetwork;
import de.mickare.net.core.Namespace;
import de.mickare.net.core.Node;
import de.mickare.net.event.NodeEvent.NodeAddedEvent;
import de.mickare.net.event.NodeEvent.NodeRemovedEvent;
import de.mickare.net.event.NodeEvent.NodeUpdatedEvent;
import lombok.Getter;

public class LeaderElection implements NetworkService {
	
	private @Getter final AbstractNetwork network;
	// private final ObjectTunnel.Descriptor<Packet> tunnelDesc;
	
	private @Getter final String name;
	private @Getter final Namespace namespace;
	private @Getter boolean registered = false;
	
	private Node leader = null;
	private @Getter long leaderTimestamp = System.currentTimeMillis();
	private long timeout; // millis
	
	/**
	 * 
	 * @param network
	 *            the network this service belongs to
	 * @param name
	 *            of service
	 * @param timeout
	 *            it takes to consolidate the leader position
	 */
	public LeaderElection( AbstractNetwork network, String name, long timeout ) {
		Preconditions.checkNotNull( network );
		Preconditions.checkArgument( !name.isEmpty() );
		Preconditions.checkArgument( timeout > 0 );
		this.network = network;
		this.name = name;
		this.namespace = new Namespace( network, "leaderelection." + name.toLowerCase() );
		
		this.timeout = timeout;
	}
	
	public final synchronized void register() {
		if ( !this.registered ) {
			this.network.getHome().addNamespace( this.namespace.getName() );
			this.network.getEventBus().register( this );
			this.registered = true;
			this.setLeader( this.calculateLeader() );
		}
	}
	
	public final synchronized void unregister() {
		if ( this.registered ) {
			this.network.getHome().removeNamespace( this.namespace.getName() );
			this.network.getEventBus().unregister( this );
			this.registered = false;
		}
	}
	
	public final boolean isLeader( UUID uuid ) {
		if ( leaderTimestamp + timeout <= System.currentTimeMillis() ) {
			return _isLeader( uuid );
		}
		return false;
	}
	
	public final boolean isLeader() {
		return isLeader( this.network.getHome().getId() );
	}
	
	public final Node getLeader() {
		if ( leaderTimestamp + timeout <= System.currentTimeMillis() ) {
			return this.leader;
		}
		return null;
	}
	
	private void resetTimeout() {
		leaderTimestamp = System.currentTimeMillis();
	}
	
	private boolean isBetterLeader( Node node ) {
		if ( !node.hasNamespace( namespace ) ) {
			return false;
		}
		return ( this.leader == null || leader.getId().compareTo( node.getId() ) > 0 );
	}
	
	private void setLeader( Node leader ) {
		if ( this.leader != leader ) {
			Node old = this.leader;
			this.resetTimeout();
			this.leader = leader;
			onLeaderChanged( old, leader );
		}
	}
	
	/**
	 * Override to get notified if the leader changed.
	 * 
	 * @param fromLeader
	 *            nullable
	 * @param toLeader
	 *            nullable
	 */
	protected void onLeaderChanged( Node fromLeader, Node toLeader ) {
	
	}
	
	private boolean _isLeader( UUID uuid ) {
		return this.leader != null ? this.leader.getId().equals( uuid ) : false;
	}
	
	private Node calculateLeader() {
		final List<Node> nodes = Lists.newArrayList( this.namespace.getNodes() );
		Collections.sort( nodes, ( a, b ) -> a.getId().compareTo( b.getId() ) );
		if ( !nodes.isEmpty() ) {
			return nodes.get( 0 );
		}
		return null;
	}
	
	@Subscribe
	public void on( NodeAddedEvent event ) {
		if ( isBetterLeader( event.getNode() ) ) {
			this.setLeader( event.getNode() );
		}
	}
	
	@Subscribe
	public void on( NodeUpdatedEvent event ) {
		if ( isBetterLeader( event.getNode() ) ) {
			this.setLeader( event.getNode() );
		} else if ( _isLeader( event.getNode().getId() ) && !event.getNode().hasNamespace( namespace ) ) {
			this.setLeader( this.calculateLeader() );
		}
	}
	
	@Subscribe
	public void on( NodeRemovedEvent event ) {
		if ( this._isLeader( event.getNode().getId() ) ) {
			this.setLeader( this.calculateLeader() );
		}
	}
	
}
