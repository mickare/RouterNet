package de.mickare.net.bukkittest;

import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import de.mickare.net.Net;
import de.mickare.net.Owner;
import de.mickare.net.core.Target;
import de.mickare.net.core.procedure.CallableRegisteredProcedure;
import de.mickare.net.core.procedure.MultiProcedureCall;
import de.mickare.net.core.procedure.Procedure;
import de.mickare.net.core.procedure.ProcedureCallResult;
import de.mickare.net.core.tunnel.SubTunnelDescriptor;
import de.mickare.net.core.tunnel.TunnelDescriptors;
import de.mickare.net.core.tunnel.object.ObjectTunnel;

public class BukkitTestPlugin extends JavaPlugin implements Owner, Listener {
	
	private static String msg_args = "§cFehler: Zu wenig argumente!";
	
	private SubTunnelDescriptor<ObjectTunnel<String>> BROADCAST = TunnelDescriptors.getObjectTunnel( "broadcast", String.class );
	private SubTunnelDescriptor<ObjectTunnel<DataPlayerPosition>> PLAYER_POSITION = TunnelDescriptors.getObjectTunnel( "playerposition", DataPlayerPosition.class );
	
	private CallableRegisteredProcedure<Void, DataPlayerList> online_players;
	private CallableRegisteredProcedure<DataPrivateMessage, Boolean> private_message;
	
	private ConcurrentHashMap<UUID, AtomicLong> stresstestCounter = new ConcurrentHashMap<>();
	
	@Override
	public void onLoad() {
	}
	
	@Override
	public void onDisable() {
		getLogger().info( getName() + " disabled!" );
	}
	
	@Override
	public void onEnable() {
		
		Net.getNetwork().getHome().addNamespace( "bukkittest" );
		this.online_players = Procedure.of( "online_players", this::getOnlinePlayers ).register();
		this.private_message = Procedure.of( "private_message", this::receivePrivateMessage ).register();
		
		Net.getTunnel( BROADCAST ).registerListener( this, ( msg ) -> {
			Bukkit.broadcastMessage( msg.getObject() );
		} );
		;
		
		Net.getTunnel( PLAYER_POSITION ).registerListener( this, ( msg ) -> {
		
		} );
		
		Net.getTunnel( "stresstest" ).registerListener( this, ( msg ) -> {
			
			getLogger().info( "Stresstest: " + stresstestCounter.computeIfAbsent( msg.getSenderId(), k -> new AtomicLong( 0 ) ).incrementAndGet() );
			
		} );
		
		Bukkit.getPluginManager().registerEvents( this, this );
		
		getLogger().info( getName() + " enabled!" );
	}
	
	public boolean stressTest( final int times ) {
		
		new BukkitRunnable() {
			@Override
			public void run() {
				Target target = Target.newBuilder().include( "bukkittest" ).exclude( Net.getNetwork().getHome() ).build();
				byte buffer[] = new byte[1024];
				Random ran = new Random();
				for ( int i = 0; i < times; ++i ) {
					ran.nextBytes( buffer );
					Net.getTunnel( "stresstest" ).send( target, buffer );
				}
			}
		}.runTaskAsynchronously( this );
		
		return true;
	}
	
	public void broadcast( String msg ) {
		Net.getTunnel( BROADCAST ).send( Target.toAll(), msg );
	}
	
	public DataPlayerList getOnlinePlayers() {
		Bukkit.getLogger().info( "getOnlinePlayers" );
		DataPlayerList result = new DataPlayerList();
		Bukkit.getOnlinePlayers().stream().map( DataPlayer::new ).forEach( result::add );
		return result;
	}
	
	@EventHandler
	public void onChat( AsyncPlayerChatEvent event ) {
		if ( !event.getMessage().startsWith( "/" ) ) {
			broadcast( event.getPlayer().getDisplayName() + ": " + event.getMessage() );
			event.setCancelled( true );
		}
	}
	
	@EventHandler
	public void onMove( PlayerMoveEvent event ) {
		// take it to the extreme!
		
		Net.getTunnel( PLAYER_POSITION ).broadcast( new DataPlayerPosition( Net.getClient().getHome().getId(), event.getPlayer() ) );
		
	}
	
	public Boolean receivePrivateMessage( DataPrivateMessage msg ) {
		Bukkit.getLogger().info( "receivePrivateMessage" );
		Player player = Bukkit.getPlayer( msg.getReceiverName() );
		if ( player != null ) {
			player.sendMessage( "[" + msg.getSenderName() + "]->[me]: " + msg.getMessage() );
			return true;
		}
		return false;
	}
	
	public boolean onCommand( final CommandSender sender, Command cmd, String commandLabel, String[] args ) {
		
		if ( cmd.getName().equalsIgnoreCase( "players" ) ) {
			return listPlayers( sender );
			
		} else if ( cmd.getName().equalsIgnoreCase( "m" ) ) {
			return sendPrivateMessage( sender, args );
			
		} else if ( cmd.getName().equalsIgnoreCase( "stresstest" ) ) {
			int times = 100;
			if ( args.length > 0 ) {
				times = Integer.parseInt( args[0] );
			}
			return stressTest( times );
		}
		return false;
	}
	
	public boolean listPlayers( final CommandSender sender ) {
		
		online_players.call( Target.to( "bukkittest" ), null, 1000 ).addListener( results -> {
			
			StringBuilder sb = new StringBuilder();
			
			for ( ProcedureCallResult<Void, DataPlayerList> result : results ) {
				
				if ( result.isSuccess() ) {
					
					sb.append( result.getNode().toString() ).append( ":\n" );
					try {
						sb.append( String.join( ", ", result.get().stream().map( DataPlayer::getDisplayName ).collect( Collectors.toList() ) ) );
						sb.append( "\n" );
					} catch ( Exception e ) {
					}
					
				} else {
					getLogger().log( Level.WARNING, "Failed to receive playerlist from: " + result.getNode().toString(), result.cause() );
				}
				
			}
			
			sender.sendMessage( sb.toString() );
		} );
		
		return true;
		
	}
	
	public boolean sendPrivateMessage( final CommandSender sender, String[] args ) {
		
		if ( args.length < 2 ) {
			sender.sendMessage( msg_args );
			return true;
		}
		
		String target = args[0];
		String message = String.join( " ", Arrays.copyOfRange( args, 1, args.length ) );
		final DataPrivateMessage msg = new DataPrivateMessage( sender.getName(), target, message );
		
		MultiProcedureCall<DataPrivateMessage, Boolean> call = private_message.call( Target.to( "bukkittest" ), msg, 1000 );
		
		call.addListenerEach( res -> {
			
			if ( res.isSuccess() && res.getUnchecked() ) {
				sender.sendMessage( "[me]->[" + target + "]: " + message );
			}
			
		} );
		
		call.addListener( results -> {
			
			boolean succeeded = results.stream().filter( r -> r.isSuccess() ).map( r -> r.getUnchecked() ).findAny().orElse( false );
			if ( !succeeded ) {
				sender.sendMessage( target + " is not online!" );
			}
			
		} );
		
		return true;
		
	}
	
}
