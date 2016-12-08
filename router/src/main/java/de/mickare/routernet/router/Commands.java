package de.mickare.routernet.router;

import de.mickare.routernet.router.command.CommandManager;
import de.mickare.routernet.router.command.ExitCommand;
import de.mickare.routernet.router.command.HelpCommand;
import de.mickare.routernet.router.command.InfoCommand;
import de.mickare.routernet.router.command.ListCommand;
import de.mickare.routernet.router.command.RamCommand;
import de.mickare.routernet.router.command.RestartCommand;
import de.mickare.routernet.router.command.TunnelCommand;

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
