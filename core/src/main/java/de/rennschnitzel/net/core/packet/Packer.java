package de.rennschnitzel.net.core.packet;

import java.util.function.BiConsumer;

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

public class Packer {
	
	private Packer() {
	}
	
	public static final ThreadLocal<Packet.Builder> PACKET_BUILDER = ThreadLocal.withInitial( Packet::newBuilder );
	
	public Packet.Builder packetBuilder() {
		return PACKET_BUILDER.get();
	}
	
	private static <V> Packet pack( final BiConsumer<Packet.Builder, V> setter, final V value ) {
		final Packet.Builder builder = PACKET_BUILDER.get();
		setter.accept( builder, value );
		final Packet packet = builder.build();
		builder.clear();
		return packet;
	}
	
	private static final BiConsumer<Packet.Builder, CloseMessage> SETTER_CLOSE = Packet.Builder::setClose;
	private static final BiConsumer<Packet.Builder, HeartbeatMessage> SETTER_HEARTBEAT = Packet.Builder::setHeartbeat;
	
	private static final BiConsumer<Packet.Builder, LoginHandshakeMessage> SETTER_LOGIN_HANDSHAKE = Packet.Builder::setLoginHandshake;
	private static final BiConsumer<Packet.Builder, LoginChallengeMessage> SETTER_LOGIN_CHALLENGE = Packet.Builder::setLoginChallenge;
	private static final BiConsumer<Packet.Builder, LoginResponseMessage> SETTER_LOGIN_RESPONSE = Packet.Builder::setLoginResponse;
	private static final BiConsumer<Packet.Builder, LoginSuccessMessage> SETTER_LOGIN_SUCCESS = Packet.Builder::setLoginSuccess;
	private static final BiConsumer<Packet.Builder, LoginUpgradeMessage> SETTER_LOGIN_UPGRADE = Packet.Builder::setLoginUpgrade;
	
	private static final BiConsumer<Packet.Builder, NodeTopologyMessage> SETTER_NODE_TOPOLOGY = Packet.Builder::setNodeTopology;
	private static final BiConsumer<Packet.Builder, NodeUpdateMessage> SETTER_NODE_UPDATE = Packet.Builder::setNodeUpdate;
	private static final BiConsumer<Packet.Builder, NodeRemoveMessage> SETTER_NODE_REMOVE = Packet.Builder::setNodeRemove;
	
	private static final BiConsumer<Packet.Builder, TunnelMessage> SETTER_TUNNEL_MESSAGE = Packet.Builder::setTunnelMessage;
	private static final BiConsumer<Packet.Builder, TunnelRegister> SETTER_TUNNEL_REGISTER = Packet.Builder::setTunnelRegister;
	
	private static final BiConsumer<Packet.Builder, ProcedureMessage> SETTER_PROCEDURE_MESSAGE = Packet.Builder::setProcedureMessage;
	
	// ******************************************************************************
	// Transport
	
	public static Packet pack( CloseMessage.Builder builder ) {
		return pack( builder.build() );
	}
	
	public static Packet pack( CloseMessage value ) {
		return pack( SETTER_CLOSE, value );
	}
	
	public static Packet pack( HeartbeatMessage.Builder builder ) {
		return pack( builder.build() );
	}
	
	public static Packet pack( HeartbeatMessage value ) {
		return pack( SETTER_HEARTBEAT, value );
	}
	
	// ******************************************************************************
	// Handshake
	
	public static Packet pack( LoginHandshakeMessage.Builder builder ) {
		return pack( builder.build() );
	}
	
	public static Packet pack( LoginHandshakeMessage value ) {
		return pack( SETTER_LOGIN_HANDSHAKE, value );
	}
	
	public static Packet pack( LoginChallengeMessage.Builder builder ) {
		return pack( builder.build() );
	}
	
	public static Packet pack( LoginChallengeMessage value ) {
		return pack( SETTER_LOGIN_CHALLENGE, value );
	}
	
	public static Packet pack( LoginResponseMessage.Builder builder ) {
		return pack( builder.build() );
	}
	
	public static Packet pack( LoginResponseMessage value ) {
		return pack( SETTER_LOGIN_RESPONSE, value );
	}
	
	public static Packet pack( LoginSuccessMessage.Builder builder ) {
		return pack( builder.build() );
	}
	
	public static Packet pack( LoginSuccessMessage value ) {
		return pack( SETTER_LOGIN_SUCCESS, value );
	}
	
	public static Packet pack( LoginUpgradeMessage.Builder builder ) {
		return pack( builder.build() );
	}
	
	public static Packet pack( LoginUpgradeMessage value ) {
		return pack( SETTER_LOGIN_UPGRADE, value );
	}
	
	// ******************************************************************************
	// Network
	
	public static Packet pack( NodeTopologyMessage.Builder builder ) {
		return pack( builder.build() );
	}
	
	public static Packet pack( NodeTopologyMessage value ) {
		return pack( SETTER_NODE_TOPOLOGY, value );
	}
	
	public static Packet pack( NodeUpdateMessage.Builder builder ) {
		return pack( builder.build() );
	}
	
	public static Packet pack( NodeUpdateMessage value ) {
		return pack( SETTER_NODE_UPDATE, value );
	}
	
	public static Packet pack( NodeRemoveMessage.Builder builder ) {
		return pack( builder.build() );
	}
	
	public static Packet pack( NodeRemoveMessage value ) {
		return pack( SETTER_NODE_REMOVE, value );
	}
	
	// ******************************************************************************
	// Tunnel
	
	public static Packet pack( TunnelMessage.Builder builder ) {
		return pack( builder.build() );
	}
	
	public static Packet pack( TunnelMessage value ) {
		return pack( SETTER_TUNNEL_MESSAGE, value );
	}
	
	public static Packet pack( TunnelRegister.Builder builder ) {
		return pack( builder.build() );
	}
	
	public static Packet pack( TunnelRegister value ) {
		return pack( SETTER_TUNNEL_REGISTER, value );
	}
	
	// ******************************************************************************
	// Procedure
	
	public static Packet pack( ProcedureMessage.Builder builder ) {
		return pack( builder.build() );
	}
	
	public static Packet pack( ProcedureMessage value ) {
		return pack( SETTER_PROCEDURE_MESSAGE, value );
	}
	
}
