package de.mickare.routernet.router.command;

import jline.console.completer.Completer;

public interface Command {

  public String getLabel();

  public String getUsage();

  public String getDescription();

  public String[] getAliases();

  public void execute(String[] args);

  public boolean hasCompleter();

  public Completer getCompleter();

}
