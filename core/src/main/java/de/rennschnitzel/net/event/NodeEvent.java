package de.rennschnitzel.net.event;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.Node;
import lombok.Getter;
import lombok.NonNull;

public class NodeEvent extends NetworkEvent {
	
	private @Getter @NonNull final Node node;
	
	public NodeEvent( AbstractNetwork network, Node node ) {
		super( network );
		Preconditions.checkNotNull( node );
		this.node = node;
	}
	
	public static class NodeAddedEvent extends NodeEvent {
		public NodeAddedEvent( AbstractNetwork network, Node node ) {
			super( network, node );
		}
	}
	
	public static class NodeUpdatedEvent extends NodeEvent {
		public NodeUpdatedEvent( AbstractNetwork network, Node node ) {
			super( network, node );
		}
	}
	
	public static class NodeRemovedEvent extends NodeEvent {
		public NodeRemovedEvent( AbstractNetwork network, Node node ) {
			super( network, node );
		}
	}
	
}
