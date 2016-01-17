package de.rennschnitzel.backbone.event;

import de.rennschnitzel.backbone.net.AbstractNetwork;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NetworkEvent {

  @Getter
  @NonNull
  private final AbstractNetwork network;


}
