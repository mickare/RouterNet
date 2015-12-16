package de.rennschnitzel.backbone.api.network.message;

import java.util.UUID;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;

import de.rennschnitzel.backbone.ProtocolUtils;
import de.rennschnitzel.backbone.api.network.target.Target;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ByteContent;
import lombok.Getter;

@Getter
public class ByteMessage extends ContentMessage {

  private final String key;
  private final ByteString data;

  public ByteMessage(Target target, UUID sender, String key, byte[] data) {
    this(target, sender, key, ByteString.copyFrom(data));
  }

  public ByteMessage(Target target, UUID sender, String key, ByteString data) {
    super(target, sender);
    Preconditions.checkArgument(!key.isEmpty());
    Preconditions.checkNotNull(data);
    this.key = key.toLowerCase();
    this.data = data;
  }

  public ByteMessage(TransportProtocol.ContentMessage msg) {
    super(msg);
    Preconditions.checkArgument(msg.getContentCase() == TransportProtocol.ContentMessage.ContentCase.BYTES);
    this.key = msg.getBytes().getKey();
    Preconditions.checkArgument(!this.key.isEmpty());
    this.data = msg.getBytes().getData();
  }

  @Override
  public TransportProtocol.ContentMessage toProtocol() {
    TransportProtocol.ContentMessage.Builder b = TransportProtocol.ContentMessage.newBuilder();
    b.setTarget(this.getTarget().toProtocol());
    b.setSender(ProtocolUtils.convert(this.getSender()));
    b.setBytes(ByteContent.newBuilder().setKey(key).setData(data));
    return b.build();
  }


}
