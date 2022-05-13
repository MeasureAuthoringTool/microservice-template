package cms.gov.madie.measure.exceptions;

public class ArgumentFailedValidationException extends RuntimeException {
  private final String key;

  public ArgumentFailedValidationException(String key, String message) {
    super(message);
    this.key = key;
  }
}
