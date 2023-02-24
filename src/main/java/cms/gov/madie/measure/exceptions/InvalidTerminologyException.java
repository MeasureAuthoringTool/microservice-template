package cms.gov.madie.measure.exceptions;

public class InvalidTerminologyException extends RuntimeException {
  private static final String MESSAGE = "Invalid terminology %s: %s";

  public InvalidTerminologyException(String message) {
    super(message);
  }

  public InvalidTerminologyException(String terminologyType, String id) {
    super(String.format(MESSAGE, terminologyType, id));
  }
}
