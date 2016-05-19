package de.rennschnitzel.net.core.tunnel.object;

import java.util.UUID;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.Target;
import de.rennschnitzel.net.core.tunnel.TunnelMessage;
import lombok.Getter;

public class ObjectTunnelMessage<T> extends TunnelMessage {
	
	private @Getter final ObjectTunnel<T> objectTunnel;
	private @Getter final T object;
	
	public ObjectTunnelMessage( ObjectTunnel<T> objectTunnel, TunnelMessage cmsg ) throws ConvertObjectTunnelException {
		super( cmsg );
		Preconditions.checkNotNull( objectTunnel );
		this.objectTunnel = objectTunnel;
		this.object = objectTunnel.getConverter().asObject( cmsg.getData() );
	}
	
	public ObjectTunnelMessage( ObjectTunnel<T> objectTunnel, final Target target, final UUID senderId, final T object ) throws ConvertObjectTunnelException {
		super( objectTunnel.getParentTunnel(), target, senderId, objectTunnel.getConverter().asByteString( object ) );
		Preconditions.checkNotNull( objectTunnel );
		this.objectTunnel = objectTunnel;
		this.object = object;
	}
	
}
