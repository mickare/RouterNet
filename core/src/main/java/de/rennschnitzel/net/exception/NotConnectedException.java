package de.rennschnitzel.net.exception;

import java.io.IOException;

public class NotConnectedException extends IOException {

  /**
   * 
   */
  private static final long serialVersionUID = -1974145666422174946L;

  public NotConnectedException() {}

  public NotConnectedException(String message) {
    super(message);
  }

  public NotConnectedException(Throwable cause) {
    super(cause);
  }

  public NotConnectedException(String message, Throwable cause) {
    super(message, cause);
  }

}
