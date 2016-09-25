package de.mickare.net.event;

import de.mickare.net.core.AbstractNetwork;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public @RequiredArgsConstructor class NetworkEvent {
	
	private @Getter @NonNull final AbstractNetwork network;
	
}
