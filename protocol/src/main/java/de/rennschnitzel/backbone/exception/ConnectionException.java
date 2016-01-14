package de.rennschnitzel.backbone.exception;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ErrorMessage;
import lombok.Getter;

@SuppressWarnings("serial")
public class ConnectionException extends Exception {

  @Getter
  private final ErrorMessage.Type type;

  @Getter
  private boolean doLog = false;


  public ConnectionException(ErrorMessage error) {
    this(error.getType(), error.getMessage());
  }

  public ConnectionException(ErrorMessage.Type type) {
    Preconditions.checkNotNull(type);
    this.type = type;
  }

  public ConnectionException(ErrorMessage.Type type, String message) {
    super(message);
    Preconditions.checkNotNull(type);
    this.type = type;
  }

  public ConnectionException(ErrorMessage.Type type, Throwable cause) {
    super(cause);
    Preconditions.checkNotNull(type);
    this.type = type;
  }

  public ConnectionException(ErrorMessage.Type type, String message, Throwable cause) {
    super(message, cause);
    Preconditions.checkNotNull(type);
    this.type = type;
  }

  public ConnectionException(ErrorMessage.Type type, String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
    Preconditions.checkNotNull(type);
    this.type = type;
  }


  public ConnectionException doLog() {
    this.doLog = true;
    return this;
  }

}
