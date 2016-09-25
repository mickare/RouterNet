package de.mickare.net.core.packet;

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

public class SimplePacketHandler<C> implements PacketHandler<C> {
	
	public SimplePacketHandler() {
	}
	
	@Override
	public void handle( C ctx, HeartbeatMessage heartbeat ) throws Exception {
		throw new UnsupportedOperationException( "Not implemented!" );
	}
	
	@Override
	public void handle( C ctx, CloseMessage msg ) throws Exception {
		throw new UnsupportedOperationException( "Not implemented!" );
	}
	
	@Override
	public void handle( C ctx, LoginHandshakeMessage msg ) throws Exception {
		throw new UnsupportedOperationException( "Not implemented!" );
	}
	
	@Override
	public void handle( C ctx, LoginResponseMessage msg ) throws Exception {
		throw new UnsupportedOperationException( "Not implemented!" );
	}
	
	@Override
	public void handle( C ctx, LoginChallengeMessage msg ) throws Exception {
		throw new UnsupportedOperationException( "Not implemented!" );
	}
	
	@Override
	public void handle( C ctx, LoginSuccessMessage msg ) throws Exception {
		throw new UnsupportedOperationException( "Not implemented!" );
	}
	
	@Override
	public void handle( C ctx, LoginUpgradeMessage msg ) throws Exception {
		throw new UnsupportedOperationException( "Not implemented!" );
	}
	
	@Override
	public void handle( C ctx, NodeTopologyMessage msg ) throws Exception {
		throw new UnsupportedOperationException( "Not implemented!" );
	}
	
	@Override
	public void handle( C ctx, NodeUpdateMessage msg ) throws Exception {
		throw new UnsupportedOperationException( "Not implemented!" );
	}
	
	@Override
	public void handle( C ctx, NodeRemoveMessage msg ) throws Exception {
		throw new UnsupportedOperationException( "Not implemented!" );
	}
	
	@Override
	public void handle( C ctx, TunnelMessage msg ) throws Exception {
		throw new UnsupportedOperationException( "Not implemented!" );
	}
	
	@Override
	public void handle( C ctx, TunnelRegister msg ) throws Exception {
		throw new UnsupportedOperationException( "Not implemented!" );
	}
	
	@Override
	public void handle( C ctx, ProcedureMessage msg ) throws Exception {
		throw new UnsupportedOperationException( "Not implemented!" );
	}
	
}
