package de.rennschnitzel.net.bukkittest;

import java.io.Serializable;

import lombok.Data;
import lombok.NonNull;

public @Data class DataPrivateMessage implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4944526836028868776L;
	
	private final @NonNull String senderName;
	private final @NonNull String receiverName;
	private final @NonNull String message;
	
}
