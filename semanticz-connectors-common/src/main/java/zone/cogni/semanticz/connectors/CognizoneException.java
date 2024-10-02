package zone.cogni.semanticz.connectors;

import org.apache.commons.lang3.StringUtils;

public class CognizoneException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public CognizoneException(String message) {
    super(message);
  }

  public CognizoneException(Throwable cause) {
    super(cause);
  }

  public static <T extends CharSequence> T failIfBlank(T value, String message) {
    boolean test = StringUtils.isBlank(value);
    if (test) {
      throw new CognizoneException(message);
    }
    return value;
  }

}
