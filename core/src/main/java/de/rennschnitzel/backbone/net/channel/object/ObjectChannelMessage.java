package de.rennschnitzel.backbone.net.channel.object;

import java.util.UUID;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.net.Target;
import de.rennschnitzel.backbone.net.channel.ChannelMessage;
import lombok.Getter;

public class ObjectChannelMessage<T> extends ChannelMessage {

  @Getter
  private final ObjectChannel<T> objectChannel;
  @Getter
  private final T object;

  public ObjectChannelMessage(ObjectChannel<T> objectChannel, ChannelMessage cmsg) throws ConvertObjectChannelException {
    super(cmsg);
    Preconditions.checkNotNull(objectChannel);
    this.objectChannel = objectChannel;
    this.object = objectChannel.getConverter().asObject(cmsg.getData());
  }

  public ObjectChannelMessage(ObjectChannel<T> objectChannel, final Target target, final UUID sender, final T object) throws ConvertObjectChannelException {
    super(objectChannel.getParentChannel(), target, sender, objectChannel.getConverter().asByteString(object));
    Preconditions.checkNotNull(objectChannel);
    this.objectChannel = objectChannel;
    this.object = object;
  }

}
