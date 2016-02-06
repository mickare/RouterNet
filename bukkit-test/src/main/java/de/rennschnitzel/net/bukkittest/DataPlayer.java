package de.rennschnitzel.net.bukkittest;

import java.io.Serializable;
import java.util.UUID;

import org.bukkit.entity.Player;

import lombok.Data;
import lombok.NonNull;

public @Data class DataPlayer implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = 1798165889853558264L;
  
  private final @NonNull UUID uuid;
  private final @NonNull String name;
  private final @NonNull String displayName;

  public DataPlayer(Player player) {
    this.uuid = player.getUniqueId();
    this.name = player.getName();
    this.displayName = player.getDisplayName();
  }

}
