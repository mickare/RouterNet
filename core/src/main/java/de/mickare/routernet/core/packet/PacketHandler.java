package de.mickare.routernet.core.packet;

import de.mickare.routernet.exception.ProtocolException;
import de.mickare.routernet.protocol.LoginProtocol.LoginChallengeMessage;
import de.mickare.routernet.protocol.LoginProtocol.LoginHandshakeMessage;
import de.mickare.routernet.protocol.LoginProtocol.LoginResponseMessage;
import de.mickare.routernet.protocol.LoginProtocol.LoginSuccessMessage;
import de.mickare.routernet.protocol.LoginProtocol.LoginUpgradeMessage;
import de.mickare.routernet.protocol.NetworkProtocol.NodeRemoveMessage;
import de.mickare.routernet.protocol.NetworkProtocol.NodeTopologyMessage;
import de.mickare.routernet.protocol.NetworkProtocol.NodeUpdateMessage;
import de.mickare.routernet.protocol.TransportProtocol.CloseMessage;
import de.mickare.routernet.protocol.TransportProtocol.HeartbeatMessage;
import de.mickare.routernet.protocol.TransportProtocol.Packet;
import de.mickare.routernet.protocol.TransportProtocol.ProcedureMessage;
import de.mickare.routernet.protocol.TransportProtocol.TunnelMessage;
import de.mickare.routernet.protocol.TransportProtocol.TunnelRegister;

public interface PacketHandler<C> {
	
	default void handle( C ctx, Packet packet ) throws Exception {
		switch ( packet.getValueCase() ) {
			// Transport
			case CLOSE:
				handle( ctx, packet.getClose() );
				break;
			case HEARTBEAT:
				handle( ctx, packet.getHeartbeat() );
				break;
			
			// Login
			case LOGINHANDSHAKE:
				handle( ctx, packet.getLoginHandshake() );
				break;
			case LOGINCHALLENGE:
				handle( ctx, packet.getLoginChallenge() );
				break;
			case LOGINRESPONSE:
				handle( ctx, packet.getLoginResponse() );
				break;
			case LOGINSUCCESS:
				handle( ctx, packet.getLoginSuccess() );
				break;
			case LOGINUPGRADE:
				handle( ctx, packet.getLoginUpgrade() );
				break;
			
			// Network
			
			case NODETOPOLOGY:
				handle( ctx, packet.getNodeTopology() );
				break;
			case NODEUPDATE:
				handle( ctx, packet.getNodeUpdate() );
				break;
			case NODEREMOVE:
				handle( ctx, packet.getNodeRemove() );
				break;
			
			// Channel
			case TUNNELMESSAGE:
				handle( ctx, packet.getTunnelMessage() );
				break;
			case TUNNELREGISTER:
				handle( ctx, packet.getTunnelRegister() );
				break;
			
			// Procedure
			case PROCEDUREMESSAGE:
				handle( ctx, packet.getProcedureMessage() );
				break;
			
			default:
				handleUndefined( ctx, packet );
		}
	}
	
	default void handleUndefined( C ctx, Packet packet ) throws Exception {
		throw new ProtocolException( "Invalid or unknown packet!" );
	}
	
	// TRANSPORT
	void handle( C ctx, CloseMessage msg ) throws Exception;
	
	void handle( C ctx, HeartbeatMessage heartbeat ) throws Exception;
	
	// LOGIN
	void handle( C ctx, LoginHandshakeMessage msg ) throws Exception;
	
	void handle( C ctx, LoginResponseMessage msg ) throws Exception;
	
	void handle( C ctx, LoginChallengeMessage msg ) throws Exception;
	
	void handle( C ctx, LoginSuccessMessage msg ) throws Exception;
	
	void handle( C ctx, LoginUpgradeMessage msg ) throws Exception;
	
	// NETWORK
	void handle( C ctx, NodeTopologyMessage msg ) throws Exception;
	
	void handle( C ctx, NodeUpdateMessage msg ) throws Exception;
	
	void handle( C ctx, NodeRemoveMessage msg ) throws Exception;
	
	// TUNNEL
	void handle( C ctx, TunnelMessage msg ) throws Exception;
	
	void handle( C ctx, TunnelRegister msg ) throws Exception;
	
	// PROCEDURE
	void handle( C ctx, ProcedureMessage msg ) throws Exception;
	
}
