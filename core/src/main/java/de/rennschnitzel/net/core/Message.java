package de.rennschnitzel.net.core;

import java.util.UUID;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.ProtocolUtils;
import de.rennschnitzel.net.protocol.ComponentsProtocol.UUIDMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.TargetMessage;
import lombok.Getter;

public @Getter class Message {
	
	protected final Target target;
	protected final UUID senderId;
	
	public Message( final Target target, final UUID senderId ) {
		Preconditions.checkNotNull( target );
		Preconditions.checkNotNull( senderId );
		this.target = target;
		this.senderId = senderId;
	}
	
	public Message( TargetMessage target, UUIDMessage senderId ) {
		this( new Target( target ), ProtocolUtils.convert( senderId ) );
	}
	
}
