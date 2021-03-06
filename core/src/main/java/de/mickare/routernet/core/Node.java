package de.mickare.routernet.core;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import de.mickare.routernet.ProtocolUtils;
import de.mickare.routernet.core.packet.PacketWriter;
import de.mickare.routernet.core.procedure.CallableRegisteredProcedure;
import de.mickare.routernet.core.procedure.Procedure;
import de.mickare.routernet.protocol.TransportProtocol;
import de.mickare.routernet.protocol.NetworkProtocol.DataBukkitMessage;
import de.mickare.routernet.protocol.NetworkProtocol.DataBungeecordMessage;
import de.mickare.routernet.protocol.NetworkProtocol.DataRouterMessage;
import de.mickare.routernet.protocol.NetworkProtocol.NodeMessage;
import de.mickare.routernet.protocol.NetworkProtocol.NodeUpdateMessage;
import de.mickare.routernet.protocol.NetworkProtocol.NodeMessage.Type;
import de.mickare.routernet.protocol.TransportProtocol.Packet;
import de.mickare.routernet.util.FutureUtils;
import de.mickare.routernet.util.collection.ConditionSet;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

public @RequiredArgsConstructor class Node {
	
	private final @Getter @NonNull UUID id;
	protected @Getter NodeMessage.Type type = NodeMessage.Type.UNRECOGNIZED;
	protected @Getter Optional<String> name = Optional.empty();
	protected final Set<String> namespaces = Collections.synchronizedSet( Sets.newHashSet() );
	protected @Getter long startTimestamp = -1;
	
	protected final ConditionSet<Procedure> procedures = ConditionSet.of( Sets.newTreeSet() );
	private volatile @Getter boolean connected = false;
	private @Getter Data data = new Data();
	private transient final Stack<Consumer<Node>> updateListeners = new Stack<>();
	
	public void addUpdateListener( Consumer<Node> listener ) {
		Preconditions.checkNotNull( listener );
		this.updateListeners.push( listener );
	}
	
	public Future<Node> newUpdatePromise() {
		Promise<Node> promise = FutureUtils.newPromise();
		addUpdateListener( node -> promise.trySuccess( node ) );
		return promise;
	}
	
	protected synchronized boolean update( NodeMessage msg ) {
		UUID id = ProtocolUtils.convert( msg.getId() );
		Preconditions.checkArgument( this.id.equals( id ) );
		if ( msg.getStartTimestamp() < this.startTimestamp ) {
			return false; // drop it
		}
		if ( connected ) {
			if ( this.type != NodeMessage.Type.UNRECOGNIZED ) {
				Preconditions.checkArgument( msg.getType() == this.type, "Type of node changed!" );
			}
		}
		this.name = Optional.ofNullable( msg.getName().isEmpty() ? null : msg.getName().toLowerCase() );
		this.type = msg.getType();
		synchronized ( this.namespaces ) {
			this.namespaces.clear();
			this.namespaces.addAll( msg.getNamespacesList() );
		}
		this.procedures.safe( s -> {
			Set<Procedure> procedures = msg.getProceduresList().stream().map( Procedure::new ).collect( Collectors.toSet() );
			this.procedures.clear();
			this.procedures.addAll( procedures );
		} );
		this.data.set( msg );
		this.startTimestamp = msg.getStartTimestamp();
		this.connected = true;
		
		notifyUpdateListeners();
		
		return true;
	}
	
	protected void notifyUpdateListeners() {
		synchronized ( this.updateListeners ) {
			while ( !this.updateListeners.isEmpty() ) {
				try {
					this.updateListeners.pop().accept( this );
				} catch ( Exception e ) {
					AbstractNetwork.getInstance().getLogger().log( Level.SEVERE, "Listener failed: " + e.getMessage(), e );
				}
			}
		}
	}
	
	protected synchronized void disconnected() {
		this.connected = false;
	}
	
	public Set<String> getNamespaces() {
		synchronized ( this.namespaces ) {
			return ImmutableSet.copyOf( namespaces );
		}
	}
	
	public Set<Procedure> getProcedures() {
		return this.procedures.immutable();
	}
	
	public boolean hasNamespace( String namespace ) {
		return this.namespaces.contains( namespace.toLowerCase() );
	}
	
	public boolean hasNamespaceAny( Collection<String> namespaces ) {
		for ( String namespace : namespaces ) {
			if ( this.hasNamespace( namespace ) ) {
				return true;
			}
		}
		return false;
	}
	
	public boolean hasNamespace( Namespace namespace ) {
		return hasNamespace( namespace.getName() );
	}
	
	public boolean hasProcedure( Procedure info ) {
		return procedures.contains( info );
	}
	
	public boolean awaitProcedure( Procedure info, long time, TimeUnit unit ) throws InterruptedException {
		return procedures.awaitContains( info, time, unit );
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( "Node[" );
		if ( name.isPresent() ) {
			sb.append( name.get() );
			sb.append( ", " );
		}
		sb.append( this.getId().toString() );
		sb.append( "]" );
		return sb.toString();
	}
	
	public boolean isPart( TransportProtocol.TargetMessage target ) {
		return isPart( new Target( target ) );
	}
	
	public boolean isPart( Target target ) {
		return target.contains( this );
	}
	
	// Data
	
	public @Getter class Data {
		private DataBukkitMessage bukkit = null;
		private DataBungeecordMessage bungeecord = null;
		private DataRouterMessage router = null;
		
		private void set( NodeMessage msg ) {
			if ( Node.this.type == Type.BUKKIT ) {
				this.bukkit = msg.getDataBukkit();
			} else if ( Node.this.type == Type.BUNGEECORD ) {
				this.bungeecord = msg.getDataBungeecord();
			} else if ( Node.this.type == Type.ROUTER ) {
				this.router = msg.getDataRouter();
			}
		}
		
		public void put( NodeMessage.Builder builder ) {
			if ( builder.getType() == Type.BUKKIT && bukkit != null ) {
				builder.setDataBukkit( bukkit );
			} else if ( builder.getType() == Type.BUNGEECORD && bungeecord != null ) {
				builder.setDataBungeecord( bungeecord );
			} else if ( builder.getType() == Type.ROUTER && router != null ) {
				builder.setDataRouter( router );
			}
		}
		
	}
	
	public NodeMessage toProtocol() {
		NodeMessage.Builder b = NodeMessage.newBuilder();
		b.setType( this.type );
		this.getData().put( b );
		b.setId( ProtocolUtils.convert( this.getId() ) );
		if ( name.isPresent() ) {
			b.setName( this.name.get() );
		}
		b.setStartTimestamp( this.startTimestamp );
		b.addAllNamespaces( this.namespaces );
		this.procedures.safeSet( s -> s.stream().map( Procedure::toProtocol ).forEach( b::addProcedures ) );
		// this.procedures.stream().map( Procedure::toProtocol ).forEach( b::addProcedures );
		return b.build();
	}
	
	// Home Node
	
	public static class HomeNode extends Node {
		
		private transient volatile @Getter boolean dirty = false;
		private transient @Getter @Setter( AccessLevel.PACKAGE ) @NonNull AbstractNetwork network = null;
		
		public HomeNode( UUID id ) {
			super( id );
			this.startTimestamp = System.currentTimeMillis();
		}
		
		public HomeNode( UUID id, Collection<String> namespaces ) {
			this( id );
			Set<String> temp = Sets.newHashSet();
			namespaces.stream().filter( n -> !n.isEmpty() ).forEach( n -> temp.add( n.toLowerCase() ) );
			this.namespaces.addAll( temp );
		}
		
		public void setType( NodeMessage.Type type ) {
			Preconditions.checkNotNull( type );
			this.type = type;
		}
		
		public void addNamespace( String namespace ) {
			Preconditions.checkArgument( !namespace.isEmpty() );
			dirty |= this.namespaces.add( namespace.toLowerCase() );
			publishChanges();
		}
		
		public void removeNamespace( String namespace ) {
			Preconditions.checkArgument( !namespace.isEmpty() );
			dirty |= this.namespaces.remove( namespace.toLowerCase() );
			publishChanges();
		}
		
		public void setName( String name ) {
			Optional<String> old = this.name;
			if ( name == null || name.isEmpty() ) {
				this.name = Optional.empty();
				if ( old.isPresent() ) {
					dirty = true;
				}
			} else {
				this.name = Optional.of( name.toLowerCase() );
				if ( !old.isPresent() || old.get().equalsIgnoreCase( name ) ) {
					dirty = true;
				}
			}
			publishChanges();
		}
		
		protected void addRegisteredProcedure( CallableRegisteredProcedure<?, ?> procedure ) {
			dirty |= this.procedures.add( procedure );
			procedure.setRegisterFuture( this.newUpdatePromise() );
			publishChanges();
		}
		
		protected void removeRegisteredProcedure( CallableRegisteredProcedure<?, ?> procedure ) {
			dirty |= this.procedures.remove( procedure );
			publishChanges();
		}
		
		public void publishChanges() {
			if ( !this.dirty || network == null ) {
				return;
			}
			if ( network.getHome() != this ) {
				throw new IllegalStateException();
			}
			network.getExecutor().schedule( () -> {
				if ( this.dirty ) {
					synchronized ( HomeNode.this ) {
						if ( this.dirty ) {
							this.dirty = false;
							network.publishHomeNodeUpdate();
						}
					}
				}
			} , 1, TimeUnit.MILLISECONDS );
		}
		
		public void sendUpdate( final Collection<? extends PacketWriter<?>> connections ) {
			final NodeUpdateMessage msg = NodeUpdateMessage.newBuilder().setNode( this.toProtocol() ).build();
			final Packet packet = Packet.newBuilder().setNodeUpdate( msg ).build();
			connections.forEach( con -> con.writeAndFlushFast( packet ) );
			notifyUpdateListeners();
		}
		
		public void sendUpdate( final PacketWriter<?> out ) {
			final NodeUpdateMessage packet = NodeUpdateMessage.newBuilder().setNode( this.toProtocol() ).build();
			out.writeAndFlushFast( packet );
			notifyUpdateListeners();
		}
		
		@Override
		public boolean update( NodeMessage msg ) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		protected synchronized void disconnected() {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append( "HomeNode[" );
			if ( name.isPresent() ) {
				sb.append( name.get() );
				sb.append( ", " );
			}
			sb.append( this.getId().toString() );
			sb.append( "]" );
			return sb.toString();
		}
		
	}
	
}
