package de.rennschnitzel.backbone.api.network.message;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;

import de.rennschnitzel.backbone.ProtocolUtils;
import de.rennschnitzel.backbone.api.network.Network;
import de.rennschnitzel.backbone.api.network.target.Target;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ObjectContent;
import lombok.Getter;

@Getter
public class ObjectMessage extends ContentMessage {

  private final String type;
  private final ByteString data;

  public ObjectMessage(Target target, Class<?> type, Object object) {
    super(target);
    Preconditions.checkArgument(type.isInstance(object));
    this.type = type.getName();
    this.data = ByteString.copyFrom(Network.FST.asByteArray(object));
  }

  public ObjectMessage(TransportProtocol.ContentMessage msg) {
    super(msg);
    Preconditions.checkArgument(msg.getContentCase() == TransportProtocol.ContentMessage.ContentCase.OBJECT);
    this.type = msg.getObject().getType();
    this.data = msg.getObject().getData();
    Preconditions.checkArgument(!this.type.isEmpty());
    Preconditions.checkNotNull(this.data);
  }

  public Class<?> getTypeClass() throws ClassNotFoundException {
    return Class.forName(type);
  }

  @Override
  public TransportProtocol.ContentMessage toProtocol() {
    TransportProtocol.ContentMessage.Builder b = TransportProtocol.ContentMessage.newBuilder();
    b.setTarget(this.getTarget().toProtocol());
    b.setSender(ProtocolUtils.convert(this.getSender()));
    b.setObject(ObjectContent.newBuilder().setType(type).setData(data));
    return b.build();
  }



}
