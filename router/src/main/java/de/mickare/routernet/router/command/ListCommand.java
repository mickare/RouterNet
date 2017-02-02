package de.mickare.routernet.router.command;

import java.util.List;

import com.google.common.base.Strings;

import de.mickare.routernet.core.Connection;
import de.mickare.routernet.router.Router;

public class ListCommand extends AbstractCommand {

  public ListCommand() {
    super("list", "list", "Lists all connections");
  }

  @Override
  public void execute(String[] args) {

    List<Connection> connections = Router.getInstance().getNetwork().getConnections();

    int maxNameLength = connections.stream().map(Connection::getName).filter(n -> n != null)
        .mapToInt(String::length).max().orElse(1);

    int maxTypeLength =
        connections.stream().mapToInt(c -> c.getNode().getType().name().length()).max().orElse(1);

    int maxIdLength = 37;
    maxNameLength = maxNameLength < 5 ? 5 : maxNameLength;
    maxTypeLength = maxTypeLength < 5 ? 5 : maxTypeLength;

    maxIdLength += 2;
    maxNameLength += 2;
    maxTypeLength += 2;

    StringBuilder sb = new StringBuilder();
    sb.append("Connections:\n");
    sb.append(Strings.padEnd("ID", maxIdLength, ' '));
    sb.append(Strings.padEnd("NAME", maxNameLength, ' '));
    sb.append(Strings.padEnd("TYPE", maxTypeLength, ' '));
    sb.append("ADDRESS");
    sb.append("\n");
    sb.append(Strings.repeat("-", maxIdLength + maxNameLength + maxTypeLength + 7));
    sb.append("\n");

    for (Connection con : connections) {
      sb.append(Strings.padEnd(con.getPeerId().toString(), maxIdLength, ' '));
      String name = con.getName();
      sb.append(Strings.padEnd(name != null ? name : "null", maxNameLength, ' '));
      sb.append(Strings.padEnd(con.getNode().getType().name(), maxTypeLength, ' '));
      sb.append(con.getChannel().getRemoteAddress().toString());

      // TrafficCounter counter = cgctsh.getTrafficCounter(con.getChannel().getChannel());
      // sb.append(counter.toString());
    }

    this.getLogger().info(sb.toString());

  }

}
