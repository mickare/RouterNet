package de.rennschnitzel.net.bukkit;

import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.rennschnitzel.net.Network;

public class NetPlugin extends JavaPlugin {

  private static final Gson GSON = new GsonBuilder().create();

  private Network network;


  @Override
  public void onEnable() {



    network = new Network();


  }

  @Override
  public void onDisable() {

  }

}
