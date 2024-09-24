package zone.cogni.semanticz.connectors;

public class CognizoneException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public static CognizoneException rethrow(Throwable exception) {
    if (exception instanceof Error) {
      throw (Error) exception;
    }
    if (exception instanceof RuntimeException) {
      throw (RuntimeException) exception;
    }
    throw new CognizoneException(exception);
  }

  public CognizoneException(Throwable cause) {
    super(cause);
  }
}
