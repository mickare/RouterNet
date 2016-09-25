package de.mickare.net.router.packet;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import de.mickare.net.ProtocolUtils;
import de.mickare.net.RouterNetwork;
import de.mickare.net.core.Connection;
import de.mickare.net.core.Node;
import de.mickare.net.core.ProcedureManager;
import de.mickare.net.core.Target;
import de.mickare.net.core.Tunnel;
import de.mickare.net.core.Node.HomeNode;
import de.mickare.net.core.packet.BasePacketHandler;
import de.mickare.net.core.packet.Packer;
import de.mickare.net.core.procedure.Procedure;
import de.mickare.net.exception.ProtocolException;
import de.mickare.net.protocol.NetworkProtocol.NodeRemoveMessage;
import de.mickare.net.protocol.NetworkProtocol.NodeTopologyMessage;
import de.mickare.net.protocol.TransportProtocol.ErrorMessage;
import de.mickare.net.protocol.TransportProtocol.Packet;
import de.mickare.net.protocol.TransportProtocol.ProcedureMessage;
import de.mickare.net.protocol.TransportProtocol.ProcedureResponseMessage;
import de.mickare.net.protocol.TransportProtocol.TunnelMessage;
import io.netty.channel.ChannelFuture;

public class RouterPacketHandler extends BasePacketHandler {

  @Override
  public void handle(Connection con, NodeTopologyMessage msg) throws Exception {
    throw new ProtocolException("invalid packet");
  }

  @Override
  public void handle(Connection con, NodeRemoveMessage msg) throws Exception {
    throw new ProtocolException("invalid packet");
  }

  @Override
  public void handle(final Connection in, final ProcedureMessage msg) throws Exception {

    final RouterNetwork net = (RouterNetwork) in.getNetwork();
    final UUID sender = ProtocolUtils.convert(msg.getSender());
    final Target target = new Target(msg.getTarget());
    final Set<Node> nodes = net.getNodes(target);
    final HomeNode home = net.getHome();

    if (home.isPart(target)) {
      net.getProcedureManager().handle(msg);
    }

    Set<UUID> nodesUUID = nodes.stream().map(Node::getId).collect(Collectors.toSet());

    // Handle unknown UUIDS
    target.getNodesInclude().stream().filter(id -> !nodesUUID.contains(id)).forEach(unknown -> {
      sendProcedureFailNotConnected(in, msg, sender, unknown);
    });

    Procedure proc = null;
    if (msg.getContentCase() == ProcedureMessage.ContentCase.CALL) {
      proc = new Procedure(msg.getCall().getProcedure());
    }

    final Packet packet = Packer.pack(msg);

    for (final Node node : nodes) {

      if (node != home && !sender.equals(node.getId())) {
        Connection out = net.getConnection(node.getId());

        if (out != null && out.isActive()) {

          if (proc != null && !node.hasProcedure(proc)) {
            sendProcedureFail(in, msg, sender, node.getId(), ErrorMessage.newBuilder()
                .setType(ErrorMessage.Type.UNDEFINED).setMessage("unregistered procedure"));
            continue;
          }

          ChannelFuture cf = out.writeAndFlush(packet);
          if (msg.getContentCase() == ProcedureMessage.ContentCase.CALL) {
            cf.addListener(f -> {
              if (!f.isSuccess()) {
                sendProcedureFailNotConnected(in, msg, sender, node.getId());
              }
            });
          }

        } else {

          if (msg.getContentCase() == ProcedureMessage.ContentCase.CALL) {
            sendProcedureFailNotConnected(in, msg, sender, node.getId());
          }

        }
      }

    }

  }

  private void sendProcedureFailNotConnected(final Connection in, final ProcedureMessage msg,
      final UUID recever, final UUID sender) {
    sendProcedureFail(in, msg, recever, sender, ErrorMessage.newBuilder()
        .setType(ErrorMessage.Type.UNAVAILABLE).setMessage("not connected"));
  }

  private void sendProcedureFail(final Connection in, final ProcedureMessage msg,
      final UUID recever, final UUID sender, ErrorMessage.Builder error) {
    ProcedureResponseMessage.Builder b = ProcedureManager.newResponse(msg.getCall());
    b.setSuccess(false);
    b.setCancelled(false);
    b.setError(error);

    ProcedureMessage.Builder m = ProcedureMessage.newBuilder();
    m.setTarget(Target.to(recever).getProtocolMessage());
    m.setSender(ProtocolUtils.convert(sender));
    m.setResponse(b);

    in.writeAndFlush(m);
  }

  @Override
  public void handle(final Connection in, final TunnelMessage msg) throws Exception {

    final RouterNetwork net = (RouterNetwork) in.getNetwork();
    final UUID sender = ProtocolUtils.convert(msg.getSender());
    final Target target = new Target(msg.getTarget());
    final Set<Node> nodes = net.getNodes(target);
    final HomeNode home = net.getHome();

    if (home.isPart(target)) {
      Tunnel tunnel = net.getTunnelById(msg.getTunnelId());
      if (tunnel != null && !tunnel.isClosed()) {
        tunnel.receiveProto(in, msg);
      }
    }

    for (Node node : nodes) {

      if (node != home && !sender.equals(node.getId())) {
        Connection out = net.getConnection(node.getId());
        if (out != null && out.hasRemoteTunnel(msg.getTunnelId())) {
          out.writeAndFlush(msg);
        }
      }

    }

  }

}
