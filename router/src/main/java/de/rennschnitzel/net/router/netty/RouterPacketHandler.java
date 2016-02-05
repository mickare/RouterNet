package de.rennschnitzel.net.router.netty;

import java.util.Set;
import java.util.UUID;

import de.rennschnitzel.net.ProtocolUtils;
import de.rennschnitzel.net.RouterNetwork;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.Node;
import de.rennschnitzel.net.core.Node.HomeNode;
import de.rennschnitzel.net.core.ProcedureManager;
import de.rennschnitzel.net.core.Target;
import de.rennschnitzel.net.core.Tunnel;
import de.rennschnitzel.net.core.packet.BasePacketHandler;
import de.rennschnitzel.net.protocol.TransportProtocol.ErrorMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureResponseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.TunnelMessage;
import io.netty.channel.ChannelFuture;

public class RouterPacketHandler extends BasePacketHandler {

  @Override
  public void handle(final Connection in, final ProcedureMessage msg) throws Exception {

    final RouterNetwork net = (RouterNetwork) in.getNetwork();
    final UUID sender = ProtocolUtils.convert(msg.getSender());
    final Set<Node> nodes = net.getNodes(new Target(msg.getTarget()));
    final HomeNode home = net.getHome();

    if (nodes.contains(home)) {
      net.getProcedureManager().handle(msg);
    }

    for (final Node node : nodes) {

      if (node != home && sender.equals(node.getId())) {
        Connection out = net.getConnection(node.getId());
                
        if (out != null) {
          ChannelFuture cf = out.write(msg);

          if (msg.getContentCase() == ProcedureMessage.ContentCase.CALL) {
            cf.addListener(f -> {
              if (!f.isSuccess()) {
                sendFailNotConnected(in, msg, node, sender);
              }
            });
          }
          
        } else {

          if (msg.getContentCase() == ProcedureMessage.ContentCase.CALL) {
            sendFailNotConnected(in, msg, node, sender);
          }

        }
      }

    }

  }

  private void sendFailNotConnected(final Connection in, final ProcedureMessage msg,
      final Node node, final UUID sender) {
    ProcedureResponseMessage.Builder b = ProcedureManager.newResponse(msg.getCall());
    b.setSuccess(false);
    b.setCancelled(false);
    b.setError(ErrorMessage.newBuilder().setType(ErrorMessage.Type.UNAVAILABLE)
        .setMessage("not connected"));

    ProcedureMessage.Builder m = ProcedureMessage.newBuilder();
    m.setTarget(Target.to(sender).getProtocolMessage());
    m.setSender(ProtocolUtils.convert(node.getId()));
    m.setResponse(b);

    in.writeAndFlush(m);
  }


  @Override
  public void handle(final Connection in, final TunnelMessage msg) throws Exception {

    final RouterNetwork net = (RouterNetwork) in.getNetwork();
    final UUID sender = ProtocolUtils.convert(msg.getSender());
    final Set<Node> nodes = net.getNodes(new Target(msg.getTarget()));
    final HomeNode home = net.getHome();

    if (nodes.contains(home)) {
      Tunnel tunnel = net.getTunnelById(msg.getTunnelId());
      if (tunnel != null && !tunnel.isClosed()) {
        tunnel.receiveProto(msg);
      }
    }

    for (Node node : nodes) {

      if (node != home && !sender.equals(node.getId())) {
        Connection out = net.getConnection(node.getId());
        if (out != null) {
          out.writeFast(msg);
        }
      }

    }

  }

}
