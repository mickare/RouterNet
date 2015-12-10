package de.rennschnitzel.backbone.api.network.message;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;

import de.rennschnitzel.backbone.api.network.Network;
import de.rennschnitzel.backbone.api.network.target.Target;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;
import lombok.Getter;

@Getter
public class ObjectMessage extends Message {

  private final String type;
  private final ByteString data;

  public ObjectMessage(Target target, Class<?> type, Object obj) {
    super(target);
    Preconditions.checkArgument(type.isInstance(obj));
    this.type = type.getName();
    this.data = ByteString.copyFrom(Network.FST.asByteArray(obj));
  }

  public ObjectMessage(TransportProtocol.ContentMessage msg) {
    super(msg);
    Preconditions.checkArgument(msg.getContentCase() == TransportProtocol.ContentMessage.ContentCase.OBJECT);
    this.type = msg.getObject().getType();
    this.data = msg.getObject().getData();
    Preconditions.checkArgument(!this.type.isEmpty());
    Preconditions.checkNotNull(this.data);
  }


}
