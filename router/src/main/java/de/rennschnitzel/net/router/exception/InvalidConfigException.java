package de.rennschnitzel.net.router.exception;

public class InvalidConfigException extends Exception {

  /**
   * Generated VersionUID
   */
  private static final long serialVersionUID = -6977861772255944354L;

  public InvalidConfigException(String msg) {
    super(msg);
  }

  public InvalidConfigException(NumberFormatException nfe) {
    super(nfe);
  }

}
