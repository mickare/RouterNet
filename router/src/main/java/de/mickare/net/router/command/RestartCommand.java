package de.mickare.net.router.command;

import java.io.File;
import java.util.concurrent.TimeUnit;

import de.mickare.net.router.Router;

/**
 * 
 * @author Michael
 *
 *         Stops the application
 *
 */
public class RestartCommand extends AbstractCommand {

  public RestartCommand() {
    super("restart", new String[] {}, "restart", "Restarts the application");
  }

  @Override
  public void execute(String[] args) {
    getLogger().info("Restarting!");
    restart();
  }
  
  public static void restart()
  {
      restart( new File( Router.getInstance().getConfig().getRestartScript() ) );
  }

  public static void restart(final File script)
  {
      try
      {
          if ( script.isFile() )
          {
              System.out.println( "Attempting to restart with " + script.getName());
              
              // This will be done AFTER the server has completely halted
              Thread shutdownHook = new Thread()
              {
                  @Override
                  public void run()
                  {
                      try
                      {
                          String os = System.getProperty( "os.name" ).toLowerCase();
                          if ( os.contains( "win" ) )
                          {
                              Runtime.getRuntime().exec( "cmd /c start " + script.getPath() );
                          } else
                          {
                              Runtime.getRuntime().exec( new String[]
                              {
                                  "sh", script.getPath()
                              } );
                          }
                      } catch ( Exception e )
                      {
                          e.printStackTrace();
                      }
                  }
              };
              shutdownHook.setDaemon( true );
              Runtime.getRuntime().addShutdownHook( shutdownHook );              
          } else
          {
              System.out.println( "Startup script '" + script.getAbsolutePath() + "' does not exist! Stopping server." );
          }
          Router.getInstance().stopAsync();
          Router.getInstance().awaitTerminated(20, TimeUnit.SECONDS);
      } catch ( Exception ex )
      {
          ex.printStackTrace();
      }
  }
  
}
