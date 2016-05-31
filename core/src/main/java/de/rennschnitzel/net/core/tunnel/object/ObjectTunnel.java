package de.rennschnitzel.net.core.tunnel.object;

import java.io.InvalidClassException;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.logging.Level;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.Owner;
import de.rennschnitzel.net.core.Tunnel;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.Target;
import de.rennschnitzel.net.core.tunnel.AbstractSubTunnel;
import de.rennschnitzel.net.core.tunnel.AbstractSubTunnelDescriptor;
import de.rennschnitzel.net.core.tunnel.SubTunnel;
import de.rennschnitzel.net.core.tunnel.SubTunnelDescriptor;
import de.rennschnitzel.net.core.tunnel.TunnelHandler;
import de.rennschnitzel.net.core.tunnel.TunnelMessage;
import de.rennschnitzel.net.protocol.TransportProtocol;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public class ObjectTunnel<T> extends AbstractSubTunnel<ObjectTunnel<T>, ObjectTunnel.Descriptor<T>>implements TunnelHandler, SubTunnel {
	
	public static class Descriptor<T> extends AbstractSubTunnelDescriptor<Descriptor<T>, ObjectTunnel<T>>implements SubTunnelDescriptor<ObjectTunnel<T>> {
		
		private @Getter final Class<T> dataClass;
		private @Getter final ObjectConverter<T> converter;
		
		public Descriptor( String name, Class<T> dataClass ) throws InvalidClassException {
			this( name, dataClass, ObjectConverters.of( dataClass ) );
		}
		
		public Descriptor( String name, Class<T> dataClass, ObjectConverter<T> converter ) {
			super( name, TransportProtocol.TunnelRegister.Type.OBJECT );
			Preconditions.checkNotNull( dataClass );
			Preconditions.checkNotNull( converter );
			this.dataClass = dataClass;
			this.converter = converter;
		}
		
		@Override
		public boolean equals( Object o ) {
			if ( this == o ) {
				return true;
			}
			if ( !( o instanceof Descriptor ) ) {
				return false;
			}
			Descriptor<?> d = ( Descriptor<?> ) o;
			return this.name.equals( d.name ) && this.dataClass.equals( d.dataClass ) && this.converter.equals( d.converter );
		}
		
		@Override
		public int hashCode() {
			return Objects.hash( name, dataClass, converter.getClass().getName() );
		}
		
		@Override
		public ObjectTunnel<T> create( Tunnel parentChannel ) {
			return new ObjectTunnel<>( parentChannel, this );
		}
		
		@SuppressWarnings( "unchecked" )
		@Override
		public ObjectTunnel<T> cast( SubTunnel channel ) {
			if ( channel == null ) {
				return null;
			}
			Preconditions.checkArgument( channel.getDescriptor() == this );
			return ( ObjectTunnel<T> ) channel;
		}
		
	}
	
	private final CopyOnWriteArraySet<RegisteredMessageListener> listeners = new CopyOnWriteArraySet<>();
	
	public ObjectTunnel( Tunnel parentTunnel, Descriptor<T> descriptor ) throws IllegalStateException {
		super( parentTunnel, descriptor );
	}
	
	public final ObjectConverter<T> getConverter() {
		return this.descriptor.getConverter();
	}
	
	public boolean broadcast( T obj ) throws ConvertObjectTunnelException {
		return this.send( Target.toAll(), obj );
	}
	
	public boolean send( Target target, T obj ) throws ConvertObjectTunnelException {
		return send( new ObjectTunnelMessage<T>( this, target, getNetwork().getHome().getId(), obj ) );
	}
	
	public boolean send( ObjectTunnelMessage<T> ocmsg ) {
		return this.parentTunnel.send( ocmsg );
	}
	
	@Override
	public void receive( final Connection con, final TunnelMessage cmsg ) throws ConvertObjectTunnelException {
		if ( this.listeners.size() > 0 ) {
			if ( con != null ) {
				this.getExectutor().orElseGet( con.getChannel().getChannel()::eventLoop ).execute( () -> {
					this.receive( new ObjectTunnelMessage<T>( this, cmsg ) );
				} );
			} else {
				this.receive( new ObjectTunnelMessage<T>( this, cmsg ) );
			}
		}
	}
	
	public void receive( ObjectTunnelMessage<T> ocmsg ) {
		this.listeners.forEach( c -> c.accept( ocmsg ) );
	}
	
	public final void registerListener( final Owner owner, final Consumer<ObjectTunnelMessage<T>> dataConsumer ) {
		listeners.add( new RegisteredMessageListener( owner, dataConsumer ) );
	}
	
	public final void unregisterListeners( final Owner owner ) {
		this.listeners.removeIf( ( l ) -> l.getOwner().equals( owner ) );
	}
	
	@Getter
	
	private @RequiredArgsConstructor class RegisteredMessageListener implements Consumer<ObjectTunnelMessage<T>> {
		
		@NonNull
		private final Owner owner;
		@NonNull
		private final Consumer<ObjectTunnelMessage<T>> delegate;
		
		@Override
		public void accept( ObjectTunnelMessage<T> cmsg ) {
			try {
				delegate.accept( cmsg );
			} catch ( Exception e ) {
				owner.getLogger().log( Level.SEVERE, "Message listener of " + owner.toString() + " threw exception: " + e.getMessage(), e );
			}
		}
	}
	
}
