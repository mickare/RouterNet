package de.rennschnitzel.net.core.tunnel;

import java.io.IOException;
import java.util.UUID;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;

import de.rennschnitzel.net.ProtocolUtils;
import de.rennschnitzel.net.core.Tunnel;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.Message;
import de.rennschnitzel.net.core.Target;
import de.rennschnitzel.net.protocol.TransportProtocol;
import lombok.Getter;

@Getter
public class TunnelMessage extends Message {

  private final Tunnel channel;
  private final ByteString data;

  public TunnelMessage(TunnelMessage cmsg) {
    super(cmsg.target, cmsg.senderId);
    this.channel = cmsg.channel;
    this.data = cmsg.data;
  }

  public TunnelMessage(final Tunnel channel, final Target target, final UUID senderId, final ByteString byteData) {
    super(target, senderId);
    Preconditions.checkNotNull(channel);
    Preconditions.checkNotNull(byteData);
    this.channel = channel;
    this.data = byteData;
  }

  public TunnelMessage(final Tunnel channel, final TransportProtocol.TunnelMessage message) {
    super(message.getTarget(), message.getSender());
    this.channel = channel;
    this.data = message.getData();
  }

  public final TransportProtocol.TunnelMessage toProtocolMessage(Connection connection) throws IOException {
    final TransportProtocol.TunnelMessage.Builder b = TransportProtocol.TunnelMessage.newBuilder();
    b.setTunnelId(connection.getTunnelId(this.channel));
    b.setTarget(this.getTarget().getProtocolMessage());
    b.setSender(ProtocolUtils.convert(channel.getNetwork().getHome().getId()));
    b.setData(this.data);
    return b.build();
  }

}
