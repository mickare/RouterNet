package de.rennschnitzel.net.event;

import de.rennschnitzel.net.core.AbstractNetwork;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NetworkEvent {

  @Getter
  @NonNull
  private final AbstractNetwork network;


}
