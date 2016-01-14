package de.rennschnitzel.backbone.net;

import java.util.UUID;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.ProtocolUtils;
import de.rennschnitzel.backbone.net.protocol.ComponentsProtocol.UUIDMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.TargetMessage;
import lombok.Getter;

@Getter
public class Message {

  protected final Target target;
  protected final UUID sender;

  public Message(final Target target, final UUID sender) {
    Preconditions.checkNotNull(target);
    Preconditions.checkNotNull(sender);
    this.target = target;
    this.sender = sender;
  }

  public Message(TargetMessage target, UUIDMessage sender) {
    this(new Target(target), ProtocolUtils.convert(sender));
  }

}
