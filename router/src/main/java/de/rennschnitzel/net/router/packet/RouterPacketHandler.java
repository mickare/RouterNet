package de.rennschnitzel.net.router.packet;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import de.rennschnitzel.net.ProtocolUtils;
import de.rennschnitzel.net.RouterNetwork;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.Node;
import de.rennschnitzel.net.core.Node.HomeNode;
import de.rennschnitzel.net.core.ProcedureManager;
import de.rennschnitzel.net.core.Target;
import de.rennschnitzel.net.core.Tunnel;
import de.rennschnitzel.net.core.packet.BasePacketHandler;
import de.rennschnitzel.net.core.packet.Packer;
import de.rennschnitzel.net.core.procedure.Procedure;
import de.rennschnitzel.net.exception.ProtocolException;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeRemoveMessage;
import de.rennschnitzel.net.protocol.NetworkProtocol.NodeTopologyMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ErrorMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.Packet;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureResponseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.TunnelMessage;
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
        tunnel.receiveProto(msg);
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
