package de.rennschnitzel.backbone.router.netty;

import de.rennschnitzel.backbone.net.channel.Channel;
import de.rennschnitzel.backbone.net.packet.BasePacketHandler;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ChannelMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ChannelRegister;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureMessage;

public class RouterPacketHandler extends BasePacketHandler<NettyConnection> {

  @Override
  public void handle(NettyConnection con, ProcedureMessage msg) throws Exception {
    if (!isReceiver(con, msg.getTarget())) {
      return; // drop packet
    }
    con.getNetwork().getProcedureManager().handle(msg);
  }

  @Override
  public void handle(NettyConnection con, ChannelMessage msg) throws Exception {
    if (!isReceiver(con, msg.getTarget())) {
      return; // drop packet
    }
    Channel channel = con.getChannel(msg.getChannelId());
    if (channel != null && !channel.isClosed()) {
      channel.receiveProto(msg);
    }
  }

}
