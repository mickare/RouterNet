package de.rennschnitzel.net.bukkit;

import java.util.concurrent.Executors;

import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.rennschnitzel.net.NetClient;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeMessage;

public class NetPlugin extends JavaPlugin {

  private final NetClient client = new NetClient(NodeMessage.Type.BUKKIT);

  @Override
  public void onLoad() {
    client.init(getLogger(), getDataFolder(), Executors.newScheduledThreadPool(1,
        new ThreadFactoryBuilder().setNameFormat("net-pool-%d").build()));
  }

  @Override
  public void onEnable() {
    try {
      client.enable();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void onDisable() {
    try {
      client.disable();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
