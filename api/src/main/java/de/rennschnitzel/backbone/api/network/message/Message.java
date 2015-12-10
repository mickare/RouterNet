package de.rennschnitzel.backbone.api.network.message;

import java.util.UUID;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.ProtocolUtils;
import de.rennschnitzel.backbone.api.network.Network;
import de.rennschnitzel.backbone.api.network.target.Target;
import de.rennschnitzel.backbone.api.network.target.TargetOrBuilder;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ContentMessage;
import lombok.Getter;

@Getter
public class Message {

  private final Target target;
  private final UUID sender;

  public Message(ContentMessage message) {
    this(new Target(message.getTarget()), ProtocolUtils.convert(message.getSender()));
  }

  public Message(TargetOrBuilder target) {
    this(target, Network.getInstance().getID());
  }

  public Message(TargetOrBuilder target, UUID sender) {
    Preconditions.checkNotNull(target);
    Preconditions.checkNotNull(sender);
    this.target = target.build();
    this.sender = sender;
  }

}
