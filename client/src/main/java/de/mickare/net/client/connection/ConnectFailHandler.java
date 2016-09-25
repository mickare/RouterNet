package de.mickare.net.client.connection;

import lombok.RequiredArgsConstructor;

public @RequiredArgsConstructor enum ConnectFailHandler {
	
	RETRY,
	RESTART,
	SHUTDOWN;
	
}
