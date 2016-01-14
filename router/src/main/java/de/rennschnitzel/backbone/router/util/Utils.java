package de.rennschnitzel.backbone.router.util;


public class Utils {

  private Utils() {}

  public static String stackTraceToString(Throwable e) {
    return stackTraceToString(e.getStackTrace());
  }

  public static String stackTraceToString(StackTraceElement[] stack) {
    StringBuilder sb = new StringBuilder();
    for (StackTraceElement element : stack) {
      sb.append(element.toString()).append(" (").append(element.getLineNumber()).append(")");
      sb.append("\n");
    }
    return sb.toString();
  }

  public static String exception(Throwable t) {
    // TODO: We should use clear manually written exceptions
    StackTraceElement[] trace = t.getStackTrace();
    return t.getClass().getSimpleName() + " : " + t.getMessage()
        + ((trace.length > 0) ? " @ " + t.getStackTrace()[0].getClassName() + ":" + t.getStackTrace()[0].getLineNumber() : "");
  }


}
