package cms.gov.madie.measure.exceptions;

public class ArgumentCannotBeNullException extends RuntimeException {
  private final String key;

  public ArgumentCannotBeNullException(String key, String message) {
    super(message);
    this.key = key;
  }
}
