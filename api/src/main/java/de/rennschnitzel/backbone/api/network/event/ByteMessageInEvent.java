package de.rennschnitzel.backbone.api.network.event;

import de.rennschnitzel.backbone.api.network.message.ByteMessage;

public class ByteMessageInEvent extends MessageInEvent<ByteMessage> {

  public ByteMessageInEvent(ByteMessage msg) {
    super(msg);
  }
  
}
