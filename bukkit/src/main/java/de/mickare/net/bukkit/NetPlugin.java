package de.mickare.net.bukkit;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.mickare.net.NetClient;
import de.mickare.net.protocol.NetworkProtocol.NodeMessage;

public class NetPlugin extends JavaPlugin {
	
	private final NetClient client = new NetClient( NodeMessage.Type.BUKKIT );
	
	@Override
	public void onLoad() {
		
		ScheduledExecutorService executor = Executors.newScheduledThreadPool( 1, new ThreadFactoryBuilder().setNameFormat( "net-pool-%d" ).build() );
		
		client.init( getLogger(), this.getDataFolder(), executor );
		
		client.setRestartFunction( Bukkit.spigot()::restart );
		client.setShutdownFunction( Bukkit::shutdown );
		client.setSyncExecutorOnlyIfNeeded( task -> Bukkit.getScheduler().runTask( this, task ) );
	}
	
	@Override
	public void onEnable() {
		try {
			client.enable();
		} catch ( Exception e ) {
			throw new RuntimeException( e );
		}
	}
	
	@Override
	public void onDisable() {
		try {
			client.disable();
		} catch ( Exception e ) {
			throw new RuntimeException( e );
		}
	}
	
}
