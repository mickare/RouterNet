package de.mickare.net.core.packet;

import java.util.UUID;

import de.mickare.net.ProtocolUtils;
import de.mickare.net.core.Connection;
import de.mickare.net.core.Tunnel;
import de.mickare.net.exception.ProtocolException;
import de.mickare.net.protocol.TransportProtocol;
import de.mickare.net.protocol.LoginProtocol.LoginChallengeMessage;
import de.mickare.net.protocol.LoginProtocol.LoginHandshakeMessage;
import de.mickare.net.protocol.LoginProtocol.LoginResponseMessage;
import de.mickare.net.protocol.LoginProtocol.LoginSuccessMessage;
import de.mickare.net.protocol.LoginProtocol.LoginUpgradeMessage;
import de.mickare.net.protocol.NetworkProtocol.NodeRemoveMessage;
import de.mickare.net.protocol.NetworkProtocol.NodeTopologyMessage;
import de.mickare.net.protocol.NetworkProtocol.NodeUpdateMessage;
import de.mickare.net.protocol.TransportProtocol.CloseMessage;
import de.mickare.net.protocol.TransportProtocol.HeartbeatMessage;
import de.mickare.net.protocol.TransportProtocol.ProcedureMessage;
import de.mickare.net.protocol.TransportProtocol.TunnelMessage;
import de.mickare.net.protocol.TransportProtocol.TunnelRegister;

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
