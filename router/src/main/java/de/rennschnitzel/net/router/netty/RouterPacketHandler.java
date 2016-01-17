package de.rennschnitzel.net.router.netty;

import de.rennschnitzel.net.core.channel.Channel;
import de.rennschnitzel.net.core.packet.BasePacketHandler;
import de.rennschnitzel.net.protocol.TransportProtocol.ChannelMessage;
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
  public void handle(NettyConnection con, ChannelMessage msg) throws Exception {

    if (isReceiver(con, msg.getTarget())) {
      Channel channel = con.getChannelIfPresent(msg.getChannelId());
      if (channel != null && !channel.isClosed()) {
        channel.receiveProto(msg);
      }
    }
    // TODO - forwarding

  }

}
