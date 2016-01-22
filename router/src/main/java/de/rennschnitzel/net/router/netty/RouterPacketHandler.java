package de.rennschnitzel.net.router.netty;

import de.rennschnitzel.net.core.Tunnel;
import de.rennschnitzel.net.core.packet.BasePacketHandler;
import de.rennschnitzel.net.protocol.TransportProtocol.TunnelMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureMessage;

public class RouterPacketHandler extends BasePacketHandler<NettyConnection> {

  @Override
  public void handle(NettyConnection con, ProcedureMessage msg) throws Exception {
    if (isReceiver(con, msg.getTarget())) {
      con.getNetwork().getProcedureManager().handle(msg);
    }
    // TODO - forwarding

  }

  @Override
  public void handle(NettyConnection con, TunnelMessage msg) throws Exception {

    if (isReceiver(con, msg.getTarget())) {
      Tunnel channel = con.getTunnelIfPresent(msg.getChannelId());
      if (channel != null && !channel.isClosed()) {
        channel.receiveProto(msg);
      }
    }
    // TODO - forwarding

  }

}
