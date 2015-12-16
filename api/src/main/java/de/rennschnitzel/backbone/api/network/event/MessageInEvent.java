package de.rennschnitzel.backbone.api.network.event;

import de.rennschnitzel.backbone.api.network.message.ContentMessage;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class MessageInEvent<M extends ContentMessage> {

  @Getter
  @NonNull
  private final M message;

}
