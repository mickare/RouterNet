package de.mickare.routernet.router.command;

import java.text.NumberFormat;

/**
 * 
 * @author Michael
 *
 *         Shows ram usage
 *
 */
public class RamCommand extends AbstractCommand {

  public RamCommand() {
    super("ram", "ram", "shows ram usage");
  }

  @Override
  public void execute(String[] args) {
    Runtime runtime = Runtime.getRuntime();

    NumberFormat format = NumberFormat.getInstance();

    StringBuilder sb = new StringBuilder();
    long maxMemory = runtime.maxMemory();
    long allocatedMemory = runtime.totalMemory();
    long freeMemory = runtime.freeMemory();

    sb.append("free memory: ").append(format.format(freeMemory / 1024)).append('\n');
    sb.append("allocated memory: ").append(format.format(allocatedMemory / 1024)).append('\n');
    sb.append("max memory: ").append(format.format(maxMemory / 1024)).append('\n');
    sb.append("total free memory: ")
        .append(format.format((freeMemory + (maxMemory - allocatedMemory)) / 1024));

    getLogger().info(sb.toString());
  }
}
