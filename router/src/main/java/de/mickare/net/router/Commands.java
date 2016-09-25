package de.mickare.net.router;

import de.mickare.net.router.command.CommandManager;
import de.mickare.net.router.command.ExitCommand;
import de.mickare.net.router.command.HelpCommand;
import de.mickare.net.router.command.InfoCommand;
import de.mickare.net.router.command.ListCommand;
import de.mickare.net.router.command.RamCommand;
import de.mickare.net.router.command.RestartCommand;
import de.mickare.net.router.command.TunnelCommand;

public class Commands {

  public static void registerCommands(CommandManager mgr) {

    // Register all commands here!
    mgr.registerCommand(new HelpCommand(mgr));
    mgr.registerCommand(new ExitCommand());
    mgr.registerCommand(new RestartCommand());
    mgr.registerCommand(new RamCommand());
    mgr.registerCommand(new TunnelCommand());
    mgr.registerCommand(new ListCommand());
    mgr.registerCommand(new InfoCommand());

  }

}
