package cms.gov.madie.measure.exceptions;

public class InvalidIdException extends RuntimeException {
  private final static String MESSAGE = "%s ID is required for %s operation on a %s. %s";

  public InvalidIdException(final String type, final String operation) {
    this(type, operation, "");
  }

  public InvalidIdException(final String type, final String operation, String message) {
    super(String.format(MESSAGE, type, operation, type, message).trim());
  }
}
