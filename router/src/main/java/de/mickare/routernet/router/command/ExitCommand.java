package de.mickare.routernet.router.command;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import de.mickare.routernet.router.Router;

/**
 * 
 * @author Michael
 *
 *         Stops the application
 *
 */
public class ExitCommand extends AbstractCommand {

  public ExitCommand() {
    super("exit", new String[] {"quit", "stop"}, "exit", "stops the application");
  }

  @Override
  public void execute(String[] args) {
    getLogger().info("Shutting down!");
    try {
      Router.getInstance().stopAsync();
      Router.getInstance().awaitTerminated(20, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      Router.getInstance().getLogger().log(Level.SEVERE, "Shutdown Problem!", e);
    }
  }

}
