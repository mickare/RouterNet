package de.mickare.routernet.core.packet;

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
