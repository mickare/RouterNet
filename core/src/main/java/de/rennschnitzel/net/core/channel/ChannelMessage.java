package de.rennschnitzel.net.core.channel;

import java.util.UUID;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;

import de.rennschnitzel.net.ProtocolUtils;
import de.rennschnitzel.net.core.Message;
import de.rennschnitzel.net.core.Target;
import de.rennschnitzel.net.protocol.TransportProtocol;
import lombok.Getter;

@Getter
public class ChannelMessage extends Message {

  private final Channel channel;
  private final ByteString data;

  private final TransportProtocol.ChannelMessage protocolMessage;

  public ChannelMessage(ChannelMessage cmsg) {
    super(cmsg.target, cmsg.senderId);
    this.channel = cmsg.channel;
    this.data = cmsg.data;
    this.protocolMessage = cmsg.protocolMessage;
  }

  public ChannelMessage(final Channel channel, final Target target, final UUID senderId, final ByteString byteData) {
    super(target, senderId);
    Preconditions.checkNotNull(channel);
    Preconditions.checkNotNull(byteData);
    this.channel = channel;
    this.data = byteData;
    this.protocolMessage = createProtocolMessage();
  }

  public ChannelMessage(final Channel channel, final TransportProtocol.ChannelMessage message) {
    super(message.getTarget(), message.getSender());
    Preconditions.checkNotNull(channel.getChannelId() == message.getChannelId());
    this.channel = channel;
    this.protocolMessage = message;
    this.data = message.getData();
  }

  private final TransportProtocol.ChannelMessage createProtocolMessage() {
    final TransportProtocol.ChannelMessage.Builder b = TransportProtocol.ChannelMessage.newBuilder();
    b.setChannelId(channel.getChannelId());
    b.setTarget(this.getTarget().getProtocolMessage());
    b.setSender(ProtocolUtils.convert(channel.getNetwork().getHome().getId()));
    b.setData(this.data);
    return b.build();
  }

}
