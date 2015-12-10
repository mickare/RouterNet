package de.rennschnitzel.backbone.api.network.event;

import de.rennschnitzel.backbone.api.network.message.Message;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class MessageInEvent<M extends Message> {

  @Getter
  @NonNull
  private final M message;

}
