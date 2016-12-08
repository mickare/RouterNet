package de.mickare.routernet.event;

import de.mickare.routernet.core.AbstractNetwork;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public @RequiredArgsConstructor class NetworkEvent {
	
	private @Getter @NonNull final AbstractNetwork network;
	
}
