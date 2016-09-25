package de.mickare.net.router.command;

import java.util.Set;

import com.google.common.base.Strings;

import de.mickare.net.core.Tunnel;
import de.mickare.net.protocol.TransportProtocol.TunnelRegister;
import de.mickare.net.router.Router;

public class TunnelCommand extends AbstractCommand {

  public TunnelCommand() {
    super("tunnel", new String[] {"tunnels"}, "tunnel <subcmd>", "Shows info about tunnels.");
  }

  @Override
  public void execute(String[] args) {

    String subcmd = "info";
    if (args.length > 0) {
      subcmd = args[0].toLowerCase();
    }

    if (subcmd.equals("info")) {

      Set<Tunnel> tunnels = Router.getInstance().getNetwork().getTunnels();

      int maxIdLength = tunnels.stream().mapToInt(Tunnel::getId)
          .map(id -> Integer.toHexString(id).length()).max().orElse(1);
      int maxNameLength =
          tunnels.stream().map(Tunnel::getName).mapToInt(String::length).max().orElse(1);

      maxIdLength = maxIdLength < 3 ? 3 : maxIdLength;
      maxNameLength = maxNameLength < 5 ? 5 : maxNameLength;

      maxIdLength += 2;
      maxNameLength += 2;

      StringBuilder sb = new StringBuilder();
      sb.append("Tunnels:\n");
      sb.append(Strings.padEnd("ID", maxIdLength, ' '));
      sb.append(Strings.padEnd("NAME", maxNameLength, ' '));
      sb.append("TYPE");
      sb.append("\n");
      sb.append(Strings.repeat("-", maxIdLength + maxNameLength + 4));

      for (Tunnel tunnel : tunnels) {
        sb.append("\n");
        sb.append(Strings.padEnd(Integer.toHexString(tunnel.getId()), maxIdLength, ' '));
        sb.append(Strings.padEnd(tunnel.getName(), maxNameLength, ' '));
        
        TunnelRegister.Type type = tunnel.getType();        
        sb.append(type.name());
      }

      this.getLogger().info(sb.toString());

    }

  }

}
