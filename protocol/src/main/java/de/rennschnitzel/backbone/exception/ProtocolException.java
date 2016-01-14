package de.rennschnitzel.backbone.exception;

import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ErrorMessage.Type;

public class ProtocolException extends ConnectionException {

  /**
   * 
   */
  private static final long serialVersionUID = -5871338075781356108L;

  public ProtocolException() {
    super(Type.PROTOCOL_ERROR);
    // TODO Auto-generated constructor stub
  }

  public ProtocolException(String message) {
    super(Type.PROTOCOL_ERROR, message);
    // TODO Auto-generated constructor stub
  }

  public ProtocolException(Throwable cause) {
    super(Type.PROTOCOL_ERROR, cause);
    // TODO Auto-generated constructor stub
  }

  public ProtocolException(String message, Throwable cause) {
    super(Type.PROTOCOL_ERROR, message, cause);
    // TODO Auto-generated constructor stub
  }

  public ProtocolException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(Type.PROTOCOL_ERROR, message, cause, enableSuppression, writableStackTrace);
    // TODO Auto-generated constructor stub
  }

}
