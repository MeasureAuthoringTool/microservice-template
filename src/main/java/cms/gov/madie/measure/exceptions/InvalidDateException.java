package cms.gov.madie.measure.exceptions;

public class InvalidDateException extends RuntimeException {
  private final String key;

  public InvalidDateException(String key, String message) {
    super(message);
    this.key = key;
  }
}
