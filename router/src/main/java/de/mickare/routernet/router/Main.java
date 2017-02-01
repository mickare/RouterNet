package de.mickare.routernet.router;

import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.fusesource.jansi.AnsiConsole;

import com.google.common.util.concurrent.Service;

import de.mickare.routernet.router.logging.LoggingOutputStream;
import de.mickare.routernet.router.logging.SimpleLogger;
import jline.console.ConsoleReader;
import jline.console.UserInterruptException;
import lombok.AllArgsConstructor;
import lombok.NonNull;

public class Main {

  public static enum ExitCode {
    NORMAL(0), EXCEPTION_STARTUP(1), EXCEPTION_COMMAND_PROCESS(2), EXCEPTION_SHUTDOWN(3);
    private final int code;

    private ExitCode(int code) {
      this.code = code;
    }

    public int code() {
      return this.code;
    }
  }

  private static final Object SYNC = new Object();

  private static ExitCode shutdown(final Router router, final ConsoleReader console,
      final SimpleLogger logger, ExitCode exitCode) {
    if (router.isRunning()) {
      try {
        router.stopAsync();
        router.awaitTerminated(30, TimeUnit.SECONDS);
        if (router.state() == Service.State.FAILED) {
          throw router.failureCause();
        }
      } catch (Throwable t) {
        logger.log(Level.SEVERE, "Exception while shutdown!", t);
        exitCode = ExitCode.EXCEPTION_SHUTDOWN;
      }
    }

    try {
      console.shutdown();
    } finally {
      try {
        logger.close();
      } finally {
      }
    }

    return exitCode;
  }

  @AllArgsConstructor
  private static class RouterShutdownHook extends Thread {
    private @NonNull final ConsoleReader console;
    private @NonNull final SimpleLogger logger;
    private @NonNull final Router router;
    private @NonNull final ExitCode startExitCode;

    @Override
    public void run() {
      shutdown(router, console, logger, startExitCode);
    }



  }

  public static void main(String[] args) throws IOException {

    final ConsoleReader console;
    final SimpleLogger logger;
    final Router router;
    ExitCode exitCode = ExitCode.NORMAL;

    // Prepare and start
    synchronized (SYNC) {

      // Init console
      AnsiConsole.systemInstall();
      console = new ConsoleReader();
      console.setHandleUserInterrupt(true);
      console.setExpandEvents(false);

      logger = new SimpleLogger(console);

      System.setErr(new PrintStream(new LoggingOutputStream(logger, Level.SEVERE), true));
      System.setOut(new PrintStream(new LoggingOutputStream(logger, Level.INFO), true));

      router = new Router(console, logger);


      // Shutdown hook
      Runtime.getRuntime()
          .addShutdownHook(new RouterShutdownHook(console, logger, router, exitCode));


      // Starting
      try {
        router.startAsync();
        router.awaitRunning(5, TimeUnit.SECONDS);
        if (router.state() == Service.State.FAILED) {
          throw router.failureCause();
        }
      } catch (Throwable t) {
        logger.log(Level.SEVERE, "Exception while starting!", t);
        if (router.failureCause() != null || router.state() == Service.State.FAILED) {
          logger.log(Level.SEVERE, "Service failed!", router.failureCause());
        }
        System.exit(ExitCode.EXCEPTION_STARTUP.code);
        return;
      }

    }


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
      }catch(UserInterruptException e) {
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Exception in main!", e);
        exitCode = ExitCode.EXCEPTION_COMMAND_PROCESS;
        System.exit(exitCode.code);
        return;
      }

    } finally {
      System.exit(shutdown(router, console, logger, exitCode).code);
    }
  }

}
