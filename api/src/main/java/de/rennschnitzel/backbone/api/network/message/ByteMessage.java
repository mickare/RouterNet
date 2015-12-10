package de.rennschnitzel.backbone.api.network.message;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;

import de.rennschnitzel.backbone.api.network.target.Target;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;
import lombok.Getter;

@Getter
public class ByteMessage extends Message {

  private final String key;
  private final ByteString data;

  public ByteMessage(Target target, String key, byte[] data) {
    this(target, key, ByteString.copyFrom(data));
  }

  public ByteMessage(Target target, String key, ByteString data) {
    super(target);
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

}
