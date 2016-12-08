package de.mickare.routernet.event;

import java.util.Optional;
import java.util.UUID;

import com.google.common.base.Preconditions;

import de.mickare.routernet.core.AbstractNetwork;
import de.mickare.routernet.core.Connection;
import de.mickare.routernet.protocol.NetworkProtocol.NodeMessage;
import de.mickare.routernet.protocol.NetworkProtocol.NodeTopologyMessage;
import lombok.Getter;

public @Getter class LoginSuccessEvent extends NetworkEvent {
	
	private static Optional<String> name( String name ) {
		return Optional.ofNullable( name != null ? ( !name.isEmpty() ? name : null ) : null );
	};
	
	private final UUID id;
	private final Optional<String> name;
	private final Connection connection;
	
	public LoginSuccessEvent( AbstractNetwork network, UUID id, Optional<String> name, Connection connection ) {
		super( network );
		Preconditions.checkNotNull( id );
		Preconditions.checkNotNull( name );
		Preconditions.checkNotNull( connection );
		this.id = id;
		this.name = name;
		this.connection = connection;
	}
	
	public LoginSuccessEvent( AbstractNetwork network, UUID id, String name, Connection connection ) {
		this( network, id, name( name ), connection );
	}
	
	public static @Getter class ClientLoginSuccessEvent extends LoginSuccessEvent {
		
		private final NodeMessage nodeMessage;
		
		public ClientLoginSuccessEvent( AbstractNetwork network, UUID id, String name, Connection connection, NodeMessage nodeMessage ) {
			this( network, id, Optional.ofNullable( name ), connection, nodeMessage );
		}
		
		public ClientLoginSuccessEvent( AbstractNetwork network, UUID id, Optional<String> name, Connection connection, NodeMessage nodeMessage ) {
			super( network, id, name, connection );
			this.nodeMessage = nodeMessage;
		}
		
		@Override
		public String toString() {
			String name = this.getName().isPresent() ? ( ", " + this.getName().get() ) : "";
			return "ClientLoginSuccessEvent(" + this.getId() + name + ")\n" + nodeMessage.toString();
		}
		
	}
	
	public static @Getter class RouterLoginSuccessEvent extends LoginSuccessEvent {
		
		private final NodeTopologyMessage nodeTopology;
		
		public RouterLoginSuccessEvent( AbstractNetwork network, UUID id, String name, Connection connection, NodeTopologyMessage nodeTopology ) {
			this( network, id, Optional.ofNullable( name ), connection, nodeTopology );
		}
		
		public RouterLoginSuccessEvent( AbstractNetwork network, UUID id, Optional<String> name, Connection connection, NodeTopologyMessage nodeTopology ) {
			super( network, id, name, connection );
			this.nodeTopology = nodeTopology;
		}
		
		@Override
		public String toString() {
			String name = this.getName().isPresent() ? ( ", " + this.getName().get() ) : "";
			return "RouterLoginSuccessEvent(" + this.getId() + name + ")\n" + nodeTopology.toString();
		}
		
	}
	
}
