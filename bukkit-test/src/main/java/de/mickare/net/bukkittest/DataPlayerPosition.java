package de.mickare.net.bukkittest;

import java.io.Serializable;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import lombok.Data;
import lombok.NonNull;

public @Data class DataPlayerPosition implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8822323784987731729L;
	
	private final @NonNull UUID server;
	private final @NonNull DataPlayer player;
	private final double x, y, z;
	private final String world;
	
	public DataPlayerPosition( UUID server, Player player ) {
		this.server = server;
		this.player = new DataPlayer( player );
		Location loc = player.getLocation();
		this.x = loc.getX();
		this.y = loc.getX();
		this.z = loc.getX();
		this.world = loc.getWorld().getName();
	}
	
}
