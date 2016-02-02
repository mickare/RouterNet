package de.rennschnitzel.net.event;

import de.rennschnitzel.net.core.AbstractNetwork;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;


public @RequiredArgsConstructor class NetworkEvent {

  private @Getter @NonNull final AbstractNetwork network;

}
