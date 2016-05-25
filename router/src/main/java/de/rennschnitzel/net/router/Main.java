package de.rennschnitzel.net.router;

import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.fusesource.jansi.AnsiConsole;

import com.google.common.util.concurrent.Service;

import de.rennschnitzel.net.router.logging.LoggingOutputStream;
import de.rennschnitzel.net.router.logging.SimpleLogger;
import jline.console.ConsoleReader;

public class Main {

  public static enum ExitCode {
    NORMAL(0),
    EXCEPTION_STARTUP(1),
    EXCEPTION_COMMAND_PROCESS(2),
    EXCEPTION_SHUTDOWN(3);
    private final int code;

    private ExitCode(int code) {
      this.code = code;
    }

    public int code() {
      return this.code;
    }
  }

  private static final Object SYNC = new Object();

  public static void main(String[] args) throws IOException {

    final ConsoleReader console;
    final SimpleLogger logger;
    final Router router;

    // Prepare and start
    synchronized (SYNC) {

      // Init console
      AnsiConsole.systemInstall();
      console = new ConsoleReader();
      console.setExpandEvents(false);

      logger = new SimpleLogger(console);

      System.setErr(new PrintStream(new LoggingOutputStream(logger, Level.SEVERE), true));
      System.setOut(new PrintStream(new LoggingOutputStream(logger, Level.INFO), true));

      router = new Router(console, logger);
      try {
        router.startAsync();
        router.awaitRunning(5, TimeUnit.SECONDS);
        if (router.state() == Service.State.FAILED) {
          throw router.failureCause();
        }
      } catch (Throwable t) {
        logger.log(Level.SEVERE, "Exception while starting!", t);
        if (router.state() == Service.State.FAILED) {
          logger.log(Level.SEVERE, "Service failed!", router.failureCause());
        }
        System.exit(ExitCode.EXCEPTION_STARTUP.code);
        return;
      }

    }

    ExitCode exitCode = ExitCode.NORMAL;

    // Process commands and shutdown
    try {
      // Process command input
      try {
        while (router.isRunning()) {
          final String line = console.readLine(">");
          if (line != null) {
            router.getCommandManager().dispatchCommand(line);
          }
        }
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Exception in main!", e);
        exitCode = ExitCode.EXCEPTION_COMMAND_PROCESS;
        System.exit(exitCode.code);
        return;
      }
      // Shutdown
      try {
        router.stopAsync();
        router.awaitTerminated(30, TimeUnit.SECONDS);
        if (router.state() == Service.State.FAILED) {
          throw router.failureCause();
        }
      } catch (Throwable t) {
        logger.log(Level.SEVERE, "Exception while shutdown!", t);
        exitCode = ExitCode.EXCEPTION_SHUTDOWN;
        System.exit(exitCode.code);
        return;
      }

    } finally {
      try {
        console.shutdown();
      } finally {
        try {
          logger.close();
        } finally {
          System.exit(exitCode.code);
        }
      }
    }    
  }

}
