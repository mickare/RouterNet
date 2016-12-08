package de.mickare.routernet.core.packet;

import java.util.UUID;

import de.mickare.routernet.ProtocolUtils;
import de.mickare.routernet.core.Connection;
import de.mickare.routernet.core.Tunnel;
import de.mickare.routernet.exception.ProtocolException;
import de.mickare.routernet.protocol.TransportProtocol;
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
import de.mickare.routernet.protocol.TransportProtocol.ProcedureMessage;
import de.mickare.routernet.protocol.TransportProtocol.TunnelMessage;
import de.mickare.routernet.protocol.TransportProtocol.TunnelRegister;

public class BasePacketHandler implements PacketHandler<Connection> {
	
	public static final BasePacketHandler DEFAULT = new BasePacketHandler();
	
	public boolean isReceiver( Connection con, TransportProtocol.TargetMessage target ) {
		return con.getNetwork().getHome().isPart( target );
	}
	
	// TRANSPORT
	
	@Override
	public void handle( Connection con, CloseMessage msg ) throws Exception {
		con.setCloseMessage( msg );
	}
	
	@Override
	public void handle( Connection con, HeartbeatMessage heartbeat ) throws Exception {
		
	}
	
	// LOGIN
	
	@Override
	public void handle( Connection con, LoginHandshakeMessage msg ) throws Exception {
		throw new ProtocolException( "Invalid packet!" );
	}
	
	@Override
	public void handle( Connection con, LoginResponseMessage msg ) throws Exception {
		throw new ProtocolException( "Invalid packet!" );
	}
	
	@Override
	public void handle( Connection con, LoginChallengeMessage msg ) throws Exception {
		throw new ProtocolException( "Invalid packet!" );
	}
	
	@Override
	public void handle( Connection con, LoginSuccessMessage msg ) throws Exception {
		throw new ProtocolException( "Invalid packet!" );
	}
	
	@Override
	public void handle( Connection con, LoginUpgradeMessage msg ) throws Exception {
		throw new ProtocolException( "Invalid packet!" );
	}
	
	// NETWORK
	
	@Override
	public void handle( Connection con, NodeTopologyMessage msg ) throws Exception {
		con.getNetwork().updateNodes( con, msg );
	}
	
	@Override
	public void handle( Connection con, NodeUpdateMessage msg ) throws Exception {
		con.getNetwork().updateNode( con, msg.getNode() );
	}
	
	@Override
	public void handle( Connection con, NodeRemoveMessage msg ) throws Exception {
		UUID id = ProtocolUtils.convert( msg.getId() );
		con.getNetwork().removeNode( id );
	}
	
	// TUNNEL
	
	@Override
	public void handle( Connection con, TunnelRegister msg ) throws Exception {
		con.receive( msg );
	}
	
	@Override
	public void handle( Connection con, TunnelMessage msg ) throws Exception {
		
		if ( !isReceiver( con, msg.getTarget() ) ) {
			return; // drop packet
		}
		Tunnel tunnel = con.getNetwork().getTunnelById( msg.getTunnelId() );
		if ( tunnel != null && !tunnel.isClosed() ) {
			tunnel.receiveProto(con, msg );
		}
	}
	
	// PROCEDURE
	
	@Override
	public void handle( Connection con, ProcedureMessage msg ) throws Exception {
		
		con.getNetwork().getProcedureManager().handle( msg );
	}
	
}
