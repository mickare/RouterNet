package de.rennschnitzel.net.router.command;

import java.util.List;

import com.google.common.base.Strings;

import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.router.Router;
import io.netty.handler.traffic.CustomGlobalChannelTrafficShapingHandler;
import io.netty.handler.traffic.TrafficCounter;

public class ListCommand extends AbstractCommand {

  public ListCommand() {
    super("list", "list", "Lists all connections");
  }

  @Override
  public void execute(String[] args) {

    List<Connection> connections = Router.getInstance().getNetwork().getConnections();

    int maxNameLength = connections.stream().map(Connection::getName).filter(n -> n != null)
        .mapToInt(String::length).max().orElse(1);

    int maxIdLength = 37;
    maxNameLength = maxNameLength < 5 ? 5 : maxNameLength;

    maxIdLength += 2;
    maxNameLength += 2;

    CustomGlobalChannelTrafficShapingHandler cgctsh =
        Router.getInstance().getMetric().getChannelTrafficHandler();

    StringBuilder sb = new StringBuilder();
    sb.append("Connections:\n");
    sb.append(Strings.padEnd("ID", maxIdLength, ' '));
    sb.append(Strings.padEnd("NAME", maxNameLength, ' '));
    sb.append("TYPE");
    sb.append("\n");
    sb.append(Strings.repeat("-", maxIdLength + maxNameLength + 4));

    for (Connection con : connections) {
      sb.append("\n");
      sb.append(Strings.padEnd(con.getPeerId().toString(), maxIdLength, ' '));
      String name = con.getName();
      sb.append(Strings.padEnd(name != null ? name : "null", maxNameLength, ' '));
      sb.append(con.getNode().getType().name());
      sb.append("\n");

      TrafficCounter counter = cgctsh.getTrafficCounter(con.getChannel().getChannel());
      sb.append(counter.toString());
    }

    this.getLogger().info(sb.toString());

  }

}
