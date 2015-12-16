package de.rennschnitzel.backbone.api.network.message;

import java.util.UUID;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.ProtocolUtils;
import de.rennschnitzel.backbone.api.network.Network;
import de.rennschnitzel.backbone.api.network.target.Target;
import de.rennschnitzel.backbone.api.network.target.TargetOrBuilder;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;
import lombok.Getter;

@Getter
public abstract class ContentMessage implements PackableMessage {

  private final Target target;
  private final UUID sender;

  public ContentMessage(TransportProtocol.ContentMessage message) {
    this(new Target(message.getTarget()), ProtocolUtils.convert(message.getSender()));
  }

  public ContentMessage(TargetOrBuilder target) {
    this(target, Network.getInstance().getID());
  }

  public ContentMessage(TargetOrBuilder target, UUID sender) {
    Preconditions.checkNotNull(target);
    Preconditions.checkNotNull(sender);
    this.target = target.build();
    this.sender = sender;
  }

  public abstract TransportProtocol.ContentMessage toProtocol();

  @Override
  public TransportProtocol.Packet toPacket() {
    return TransportProtocol.Packet.newBuilder().setMessage(this.toProtocol()).build();
  }

}
