package de.rennschnitzel.net.core.packet;

import de.rennschnitzel.net.protocol.LoginProtocol.LoginChallengeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginHandshakeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginResponseMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginSuccessMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginUpgradeMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeRemoveMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeTopologyMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeUpdateMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.HeartbeatMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.TunnelMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.TunnelRegister;
import io.netty.util.concurrent.Future;

public interface PacketWriterDefault<F extends Future<?>> {
	
	F write( Packet packet );
	
	default F write( Packet.Builder builder ) {
		return write( builder.build() );
	}
	
	void flush();
	
	F writeAndFlush( Packet packet );
	
	default F writeAndFlush( Packet.Builder builder ) {
		return writeAndFlush( builder.build() );
	}
	
	// ******************************************************************************
	// Transport
	
	default F write( CloseMessage.Builder builder ) {
		return write( builder.build() );
	}
	
	default F write( CloseMessage value ) {
		return write( Packer.pack( value ) );
	}
	
	default F writeAndFlush( CloseMessage.Builder builder ) {
		return writeAndFlush( builder.build() );
	}
	
	default F writeAndFlush( CloseMessage value ) {
		return writeAndFlush( Packer.pack( value ) );
	}
	
	default F write( HeartbeatMessage.Builder builder ) {
		return write( builder.build() );
	}
	
	default F write( HeartbeatMessage value ) {
		return write( Packer.pack( value ) );
	}
	
	default F writeAndFlush( HeartbeatMessage.Builder builder ) {
		return writeAndFlush( builder.build() );
	}
	
	default F writeAndFlush( HeartbeatMessage value ) {
		return writeAndFlush( Packer.pack( value ) );
	}
	
	// ******************************************************************************
	// Handshake
	
	default F write( LoginHandshakeMessage.Builder builder ) {
		return write( builder.build() );
	}
	
	default F write( LoginHandshakeMessage value ) {
		return write( Packer.pack( value ) );
	}
	
	default F writeAndFlush( LoginHandshakeMessage.Builder builder ) {
		return writeAndFlush( builder.build() );
	}
	
	default F writeAndFlush( LoginHandshakeMessage value ) {
		return writeAndFlush( Packer.pack( value ) );
	}
	
	default F write( LoginChallengeMessage.Builder builder ) {
		return write( builder.build() );
	}
	
	default F write( LoginChallengeMessage value ) {
		return write( Packer.pack( value ) );
	}
	
	default F writeAndFlush( LoginChallengeMessage.Builder builder ) {
		return writeAndFlush( builder.build() );
	}
	
	default F writeAndFlush( LoginChallengeMessage value ) {
		return writeAndFlush( Packer.pack( value ) );
	}
	
	default F write( LoginResponseMessage.Builder builder ) {
		return write( builder.build() );
	}
	
	default F write( LoginResponseMessage value ) {
		return write( Packer.pack( value ) );
	}
	
	default F writeAndFlush( LoginResponseMessage.Builder builder ) {
		return writeAndFlush( builder.build() );
	}
	
	default F writeAndFlush( LoginResponseMessage value ) {
		return writeAndFlush( Packer.pack( value ) );
	}
	
	default F write( LoginSuccessMessage.Builder builder ) {
		return write( builder.build() );
	}
	
	default F write( LoginSuccessMessage value ) {
		return write( Packer.pack( value ) );
	}
	
	default F writeAndFlush( LoginSuccessMessage.Builder builder ) {
		return writeAndFlush( builder.build() );
	}
	
	default F writeAndFlush( LoginSuccessMessage value ) {
		return writeAndFlush( Packer.pack( value ) );
	}
	
	default F write( LoginUpgradeMessage.Builder builder ) {
		return write( builder.build() );
	}
	
	default F write( LoginUpgradeMessage value ) {
		return write( Packer.pack( value ) );
	}
	
	default F writeAndFlush( LoginUpgradeMessage.Builder builder ) {
		return writeAndFlush( builder.build() );
	}
	
	default F writeAndFlush( LoginUpgradeMessage value ) {
		return writeAndFlush( Packer.pack( value ) );
	}
	
	// ******************************************************************************
	// Network
	
	default F write( NodeTopologyMessage.Builder builder ) {
		return write( builder.build() );
	}
	
	default F write( NodeTopologyMessage value ) {
		return write( Packer.pack( value ) );
	}
	
	default F writeAndFlush( NodeTopologyMessage.Builder builder ) {
		return writeAndFlush( builder.build() );
	}
	
	default F writeAndFlush( NodeTopologyMessage value ) {
		return writeAndFlush( Packer.pack( value ) );
	}
	
	default F write( NodeUpdateMessage.Builder builder ) {
		return write( builder.build() );
	}
	
	default F write( NodeUpdateMessage value ) {
		return write( Packer.pack( value ) );
	}
	
	default F writeAndFlush( NodeUpdateMessage.Builder builder ) {
		return writeAndFlush( builder.build() );
	}
	
	default F writeAndFlush( NodeUpdateMessage value ) {
		return writeAndFlush( Packer.pack( value ) );
	}
	
	default F write( NodeRemoveMessage.Builder builder ) {
		return write( builder.build() );
	}
	
	default F write( NodeRemoveMessage value ) {
		return write( Packer.pack( value ) );
	}
	
	default F writeAndFlush( NodeRemoveMessage.Builder builder ) {
		return writeAndFlush( builder.build() );
	}
	
	default F writeAndFlush( NodeRemoveMessage value ) {
		return writeAndFlush( Packer.pack( value ) );
	}
	
	// ******************************************************************************
	// Tunnel
	
	default F write( TunnelMessage.Builder builder ) {
		return write( builder.build() );
	}
	
	default F write( TunnelMessage value ) {
		return write( Packer.pack( value ) );
	}
	
	default F writeAndFlush( TunnelMessage.Builder builder ) {
		return writeAndFlush( builder.build() );
	}
	
	default F writeAndFlush( TunnelMessage value ) {
		return writeAndFlush( Packer.pack( value ) );
	}
	
	default F write( TunnelRegister.Builder builder ) {
		return write( builder.build() );
	}
	
	default F write( TunnelRegister value ) {
		return write( Packer.pack( value ) );
	}
	
	default F writeAndFlush( TunnelRegister.Builder builder ) {
		return writeAndFlush( builder.build() );
	}
	
	default F writeAndFlush( TunnelRegister value ) {
		return writeAndFlush( Packer.pack( value ) );
	}
	
	// ******************************************************************************
	// Procedure
	
	default F write( ProcedureMessage.Builder builder ) {
		return write( builder.build() );
	}
	
	default F write( ProcedureMessage value ) {
		return write( Packer.pack( value ) );
	}
	
	default F writeAndFlush( ProcedureMessage.Builder builder ) {
		return writeAndFlush( builder.build() );
	}
	
	default F writeAndFlush( ProcedureMessage value ) {
		return writeAndFlush( Packer.pack( value ) );
	}
	
}
