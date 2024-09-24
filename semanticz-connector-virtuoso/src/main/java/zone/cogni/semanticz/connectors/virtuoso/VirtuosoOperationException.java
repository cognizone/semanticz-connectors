package zone.cogni.semanticz.connectors.virtuoso;

public class VirtuosoOperationException extends RuntimeException {

  private static final long serialVersionUID = -2605844968942214128L;

  public VirtuosoOperationException() {
  }

  public VirtuosoOperationException(Throwable cause) {
    super(cause);
  }

  public VirtuosoOperationException(String shortMessage, String longMessage) {
    super(shortMessage);
  }
}
