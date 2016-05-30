package de.mickare.metricweb.metric;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import de.mickare.metricweb.Service;
import de.rennschnitzel.net.router.plugin.Plugin;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public abstract class TimedPushService extends Service implements Stoppable {

  private static @RequiredArgsConstructor class TimerTask implements Runnable {
    private @NonNull final TimedPushService service;

    @Override
    public void run() {
      if (!service.running) {
        return;
      }
      try {
        service.runOneIteration();
      } catch (Exception e) {
        service.getPlugin().getLogger().log(Level.WARNING, "Exception while pushing", e);
      }
    }
  }

  private @Getter final Plugin plugin;

  private final TimerTask timer;
  private @Getter volatile boolean running = false;
  private ScheduledFuture<?> scheduledFuture = null;

  public TimedPushService(Plugin plugin, String name) {
    super(name);
    this.plugin = plugin;
    this.timer = new TimerTask(this);
  }


  public void start() {
    start(1, 1);
  }

  public synchronized void start(long delay, long period) {
    if (running) {
      return;
    }
    this.scheduledFuture = plugin.getRouter().getScheduler().scheduleAtFixedRate(timer, delay,
        period, TimeUnit.SECONDS);
    running = true;
  }

  public synchronized void stop() {
    if (!running) {
      return;
    }
    if (scheduledFuture != null) {
      scheduledFuture.cancel(true);
    }
    running = false;
  }

  protected abstract void runOneIteration() throws Exception;

}
