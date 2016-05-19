package de.rennschnitzel.net.bungee;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.rennschnitzel.net.NetClient;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeMessage;
import net.md_5.bungee.api.plugin.Plugin;

public class NetPlugin extends Plugin {
	
	private final NetClient client = new NetClient( NodeMessage.Type.BUNGEECORD );
	
	@Override
	public void onLoad() {
		
		ScheduledExecutorService executor = Executors.newScheduledThreadPool( 1, new ThreadFactoryBuilder().setNameFormat( "net-pool-%d" ).build() );
		
		client.init( getLogger(), this.getDataFolder(), executor );
		
		// client.setRestartFunction(Bukkit.spigot()::restart);
		// client.setShutdownFunction(Bukkit::shutdown);
		// client.setSyncExecutor( task -> getProxy().getScheduler().runAsync( this, task ) );
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
