package de.mickare.net.util;

import java.util.logging.Logger;

import de.mickare.net.Owner;
import lombok.Data;
import lombok.NonNull;

public @Data class SimpleOwner implements Owner {
	
	private @NonNull final String name;
	private @NonNull final Logger logger;
	
}