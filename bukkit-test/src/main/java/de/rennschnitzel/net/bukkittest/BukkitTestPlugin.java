package de.rennschnitzel.net.bukkittest;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import de.rennschnitzel.net.Net;
import de.rennschnitzel.net.Owner;
import de.rennschnitzel.net.core.Target;
import de.rennschnitzel.net.core.procedure.CallableRegisteredProcedure;
import de.rennschnitzel.net.core.procedure.MultiProcedureCall;
import de.rennschnitzel.net.core.procedure.Procedure;
import de.rennschnitzel.net.core.procedure.ProcedureCallResult;
import de.rennschnitzel.net.core.tunnel.SubTunnelDescriptor;
import de.rennschnitzel.net.core.tunnel.TunnelDescriptors;
import de.rennschnitzel.net.core.tunnel.object.ObjectTunnel;

public class BukkitTestPlugin extends JavaPlugin implements Owner, Listener {

  private static String msg_args = "§cFehler: Zu wenig argumente!";

  @Override
  public void onLoad() {}

  @Override
  public void onDisable() {
    getLogger().info(getName() + " disabled!");

  }

  private SubTunnelDescriptor<ObjectTunnel<String>> BROADCAST =
      TunnelDescriptors.getObjectTunnel("broadcast", String.class);

  private CallableRegisteredProcedure<Void, DataPlayerList> online_players;
  private CallableRegisteredProcedure<DataPrivateMessage, Boolean> private_message;

  @Override
  public void onEnable() {
    getLogger().info(getName() + " enabled!");

    Net.getNetwork().getHome().addNamespace("bukkittest");
    this.online_players = Procedure.of("online_players", this::getOnlinePlayers).register();
    this.private_message = Procedure.of("private_message", this::receivePrivateMessage).register();

    Net.getTunnel(BROADCAST).registerMessageListener(this, (msg) -> {
      Bukkit.broadcastMessage(msg.getObject());
    });;

    Bukkit.getPluginManager().registerEvents(this, this);

  }

  public void broadcast(String msg) {
    Net.getTunnel(BROADCAST).send(Target.toAll(), msg);
  }

  public DataPlayerList getOnlinePlayers() {
    DataPlayerList result = new DataPlayerList();
    Bukkit.getOnlinePlayers().stream().map(DataPlayer::new).forEach(result::add);
    return result;
  }

  @EventHandler
  public void onChat(AsyncPlayerChatEvent event) {
    if (!event.getMessage().startsWith("/")) {
      broadcast(event.getPlayer().getDisplayName() + ": " + event.getMessage());
      event.setCancelled(true);
    }
  }

  public Boolean receivePrivateMessage(DataPrivateMessage msg) {
    Player player = Bukkit.getPlayer(msg.getReceiverName());
    if (player != null) {
      player.sendMessage("[" + msg.getSenderName() + "]->[me]: " + msg.getMessage());
      return true;
    }
    return false;
  }

  public boolean onCommand(final CommandSender sender, Command cmd, String commandLabel,
      String[] args) {
    if (cmd.getName().equalsIgnoreCase("players")) {

      online_players.call(Target.to("bukkittest"), null, 500).addListener(results -> {
        StringBuilder sb = new StringBuilder();
        for (ProcedureCallResult<Void, DataPlayerList> result : results) {
          if (result.isSuccess()) {
            sb.append(result.getNode().toString()).append(":\n");
            try {
              sb.append(String.join(", ", result.get().stream().map(DataPlayer::getDisplayName)
                  .collect(Collectors.toList())));
            } catch (Exception e) {
            }
          }
        }
        sender.sendMessage(sb.toString());
      });

      return true;
    } else if (cmd.getName().equalsIgnoreCase("m")) {

      if (args.length < 2) {
        sender.sendMessage(msg_args);
        return true;
      }

      String target = args[0];
      String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
      final DataPrivateMessage msg = new DataPrivateMessage(sender.getName(), target, message);

      MultiProcedureCall<DataPrivateMessage, Boolean> call =
          private_message.call(Target.to("bukkittest"), msg, 500);
      call.addListenerEach(res -> {
        if (res.isSuccess()) {
          sender.sendMessage("[me]->[" + target + "]: " + message);
        }
      });
      call.addListener(results -> {
        boolean succeeded =
            results.stream().filter(r -> r.isSuccess()).map(r -> r.getUnchecked()).findAny().orElse(false);
        if (!succeeded) {
          sender.sendMessage(target + " is not online!");
        }
      });


      return true;
    }
    return false;
  }


}
