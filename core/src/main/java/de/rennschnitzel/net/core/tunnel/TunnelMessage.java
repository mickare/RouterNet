package de.rennschnitzel.net.core.tunnel;

import java.util.UUID;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;

import de.rennschnitzel.net.ProtocolUtils;
import de.rennschnitzel.net.core.Message;
import de.rennschnitzel.net.core.Target;
import de.rennschnitzel.net.core.Tunnel;
import de.rennschnitzel.net.protocol.TransportProtocol;
import lombok.Getter;

@Getter
public class TunnelMessage extends Message {

  private final Tunnel tunnel;
  private final ByteString data;

  public TunnelMessage(TunnelMessage cmsg) {
    super(cmsg.target, cmsg.senderId);
    this.tunnel = cmsg.tunnel;
    this.data = cmsg.data;
  }

  public TunnelMessage(final Tunnel tunnel, final Target target, final UUID senderId, final ByteString byteData) {
    super(target, senderId);
    Preconditions.checkNotNull(tunnel);
    Preconditions.checkNotNull(byteData);
    this.tunnel = tunnel;
    this.data = byteData;
  }

  public TunnelMessage(final Tunnel tunnel, final TransportProtocol.TunnelMessage message) {
    super(message.getTarget(), message.getSender());
    this.tunnel = tunnel;
    this.data = message.getData();
  }

  public final TransportProtocol.TunnelMessage toProtocolMessage() {
    final TransportProtocol.TunnelMessage.Builder b = TransportProtocol.TunnelMessage.newBuilder();
    b.setTunnelId(tunnel.getId());
    b.setTarget(this.getTarget().getProtocolMessage());
    b.setSender(ProtocolUtils.convert(tunnel.getNetwork().getHome().getId()));
    b.setData(this.data);
    return b.build();
  }

}
