package de.rennschnitzel.net.router.command;

import java.util.logging.Logger;

import jline.console.completer.Completer;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.router.Main;
import de.rennschnitzel.net.router.Router;

public abstract class AbstractCommand implements Command {

  private final String label, usage, description;

  private final String[] aliases;

  private Completer completer = null;

  public AbstractCommand(String label, String usage, String description) {
    this(label, new String[0], usage, description);
  }

  public AbstractCommand(String label, String[] aliases, String usage, String description) {
    Preconditions.checkNotNull(label);
    Preconditions.checkArgument(!label.isEmpty());
    this.label = label;
    this.aliases = aliases;
    this.usage = usage;
    this.description = description;
  }

  @Override
  public String getLabel() {
    return label;
  }

  @Override
  public String[] getAliases() {
    return aliases;
  }

  public Logger getLogger() {
    return Router.getInstance().getLogger();
  }

  public String getUsage() {
    return usage;
  }

  public String getDescription() {
    return description;
  }

  protected Completer setCompleter(Completer completer) {
    Completer old = this.completer;
    this.completer = completer;
    return old;
  }

  public boolean hasCompleter() {
    return this.completer != null;
  }

  public Completer getCompleter() {
    return this.completer;
  }

}
