package de.rennschnitzel.net.router.command;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import de.rennschnitzel.net.router.Commands;
import de.rennschnitzel.net.router.Router;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.Completer;

public class CommandManager {

  private final Router main;

  private final Map<String, Command> commands = Collections.synchronizedMap(Maps.newHashMap());
  private final SortedSet<String> commandsLabels = new TreeSet<String>();

  private final Completer commandCompleter = new CommandCompleter();

  public CommandManager(Router main) {
    this.main = main;

    Commands.registerCommands(this);
    
    main.getConsole().addCompleter(commandCompleter);

  }

  public void registerCommand(Command command) {
    String label = command.getLabel().toLowerCase();

    if (getCommands().containsKey(label)) {
      throw new IllegalStateException("Command \"" + label + "\" already registered!");
    }
    this.commands.put(label, command);
    this.commandsLabels.add(label);
    for (String _alias : command.getAliases()) {
      String alias = _alias.toLowerCase();
      if (!this.commands.containsKey(alias)) {
        this.commands.put(alias, command);
        this.commandsLabels.add(alias);
      }
    }
  }

  public void dispatchCommand(String commandLine) {
    String[] cmdArgs = commandLine.split("(\\s+)");

    if (cmdArgs.length == 0 || cmdArgs[0].isEmpty()) {
      getLogger().warning("No command!");
      return;
    }

    Command command = getCommands().get(cmdArgs[0].toLowerCase());
    if (command == null) {
      getLogger().warning("Command not found!");
      return;
    }

    try {
      command.execute(
          cmdArgs.length == 1 ? new String[0] : Arrays.copyOfRange(cmdArgs, 1, cmdArgs.length));
    } catch (Exception e) {
      getLogger().log(Level.WARNING, "Command Error!", e);
    }
  }

  public Logger getLogger() {
    return this.main.getLogger();
  }

  public Map<String, Command> getCommands() {
    return ImmutableMap.copyOf(commands);
  }

  private class CommandCompleter implements Completer {
    // Commands Complete
    // Copied and modified the default
    // jline.console.completer.StringsCompleter
    @Override
    public int complete(final String buffer, final int cursor,
        final List<CharSequence> candidates) {

      // buffer could be null
      Preconditions.checkNotNull(candidates);

      if (buffer == null || buffer.isEmpty()) {
        candidates.addAll(commandsLabels);
      } else {

        // The ArgumentCompleter is a nice tool to analyze the buffer.
        ArgumentCompleter.ArgumentDelimiter delim =
            new ArgumentCompleter.WhitespaceArgumentDelimiter();
        ArgumentCompleter.ArgumentList list = delim.delimit(buffer, cursor);

        if (list.getArguments().length == 0) {
          return -1;
        }
        // First we need the command!
        final String cmd = list.getArguments()[0].toLowerCase();
        if (cmd.isEmpty()) {
          return -1;
        }

        if (list.getCursorArgumentIndex() == 0) {
          // First Position is command
          for (String match : commandsLabels.tailSet(cmd)) {
            if (!match.startsWith(cmd)) {
              break;
            }
            candidates.add(match);
          }
        } else {
          // Other fields are arguments that are passed to the
          // completer of the commands.

          Command command = getCommands().get(cmd);
          if (command == null) {
            return -1;
          }

          if (command.hasCompleter()) {
            // Substract command label and the whitespace character
            // from cursor pointer
            int newCursor = cursor - cmd.length() - 1;

            // Do complete
            int ret = command.getCompleter().complete(
                String.join(" ",
                    Arrays.copyOfRange(list.getArguments(), 1, list.getArguments().length)),
                newCursor, candidates);

            int pos = cmd.length() + 1 + ret;

            /*
             * if ( ( cursor != buffer.length() ) && delim.isDelimiter( buffer, cursor ) ) { for (
             * int i = 0; i < candidates.size(); i++ ) { CharSequence val = candidates.get( i );
             * 
             * while ( val.length() > 0 && delim.isDelimiter( val, val.length() - 1 ) ) { val =
             * val.subSequence( 0, val.length() - 1 ); }
             * 
             * candidates.set( i, val ); } }
             */

            return pos;

          } else {
            return -1;
          }

        }
      }
      if (candidates.size() == 1) {
        candidates.set(0, candidates.get(0) + " ");
      }
      return candidates.isEmpty() ? -1 : 0;
    }
  }

}
