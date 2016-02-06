package de.rennschnitzel.net.router;

import de.rennschnitzel.net.router.command.CommandManager;
import de.rennschnitzel.net.router.command.ExitCommand;
import de.rennschnitzel.net.router.command.HelpCommand;
import de.rennschnitzel.net.router.command.InfoCommand;
import de.rennschnitzel.net.router.command.ListCommand;
import de.rennschnitzel.net.router.command.RamCommand;
import de.rennschnitzel.net.router.command.TunnelCommand;

public class Commands {

  public static void registerCommands(CommandManager mgr) {

    // Register all commands here!
    mgr.registerCommand(new HelpCommand(mgr));
    mgr.registerCommand(new ExitCommand());
    mgr.registerCommand(new RamCommand());
    mgr.registerCommand(new TunnelCommand());
    mgr.registerCommand(new ListCommand());
    mgr.registerCommand(new InfoCommand());

  }


}
