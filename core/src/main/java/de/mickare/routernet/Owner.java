package de.mickare.routernet;

import java.util.logging.Logger;

public interface Owner {
	
	/**
	 * Get the name of the owner.
	 * @return name
	 */
	String getName();
	
	/**
	 * Logger that should be used by components that belong to this owner.
	 * @return logger
	 */
	Logger getLogger();
	
}
