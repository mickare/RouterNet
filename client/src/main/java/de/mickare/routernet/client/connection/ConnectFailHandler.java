package de.mickare.routernet.client.connection;

import lombok.RequiredArgsConstructor;

public @RequiredArgsConstructor enum ConnectFailHandler {
	
	RETRY,
	RESTART,
	SHUTDOWN;
	
}
