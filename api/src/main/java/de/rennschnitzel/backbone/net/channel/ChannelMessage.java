package de.rennschnitzel.backbone.net.channel;

import java.util.UUID;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;

import de.rennschnitzel.backbone.ProtocolUtils;
import de.rennschnitzel.backbone.net.Target;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;
import lombok.Getter;

@Getter
public class ChannelMessage {

  private final Channel channel;
  private final Target target;
  private final UUID sender;
  private final ByteString byteData;

  private final TransportProtocol.ChannelMessage protocolMessage;

  public ChannelMessage(ChannelMessage cmsg) {
    this.channel = cmsg.channel;
    this.target = cmsg.target;
    this.sender = cmsg.sender;
    this.byteData = cmsg.byteData;
    this.protocolMessage = cmsg.protocolMessage;
  }

  public ChannelMessage(final Channel channel, final Target target, final UUID sender, final ByteString byteData) {
    Preconditions.checkNotNull(channel);
    Preconditions.checkNotNull(target);
    Preconditions.checkNotNull(sender);
    Preconditions.checkNotNull(byteData);
    this.channel = channel;
    this.target = target;
    this.sender = sender;
    this.byteData = byteData;
    this.protocolMessage = createProtocolMessage();
  }

  public ChannelMessage(final Channel channel, final TransportProtocol.ChannelMessage message) {
    Preconditions.checkNotNull(channel.getChannelId() == message.getChannelId());
    this.channel = channel;
    this.protocolMessage = message;
    this.target = new Target(message.getTarget());
    this.sender = ProtocolUtils.convert(message.getSender());
    this.byteData = message.getData();
  }

  private final TransportProtocol.ChannelMessage createProtocolMessage() {
    final TransportProtocol.ChannelMessage.Builder b = TransportProtocol.ChannelMessage.newBuilder();
    b.setChannelId(channel.getChannelId());
    b.setTarget(this.getTarget().getProtocolMessage());
    b.setSender(ProtocolUtils.convert(channel.getHome().getId()));
    b.setData(this.byteData);
    return b.build();
  }

}
