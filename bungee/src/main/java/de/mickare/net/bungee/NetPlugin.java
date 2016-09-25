package de.mickare.net.bungee;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.mickare.net.NetClient;
import de.mickare.net.protocol.NetworkProtocol.NodeMessage;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.GroupedThreadFactory;

@SuppressWarnings( "deprecation" )
public class NetPlugin extends Plugin {

	private final NetClient client = new NetClient( NodeMessage.Type.BUNGEECORD );

	@Override
	public void onLoad() {

		String name = ( getDescription() == null ) ? "unknown" : getDescription().getName();
		ScheduledExecutorService executor = Executors.newScheduledThreadPool( 1, new ThreadFactoryBuilder()//
				.setNameFormat( "net-pool-%d" )//
				// This may be needed to fake "Bungeecord tasks". ;)
				.setThreadFactory( new GroupedThreadFactory( this, name ) )//
				.build() );

		client.init( getLogger(), this.getDataFolder(), executor );

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
