package de.rennschnitzel.net.service;

import de.rennschnitzel.net.core.AbstractNetwork;

public interface NetworkService {
	
	AbstractNetwork getNetwork();
	
	/**
	 * Gets the name of service
	 * 
	 * @return name
	 */
	String getName();
	
	/**
	 * Registers the service to the network and activates it.
	 */
	void register();
	
	/**
	 * Unregisters the service from network.
	 */
	void unregister();
	
}
