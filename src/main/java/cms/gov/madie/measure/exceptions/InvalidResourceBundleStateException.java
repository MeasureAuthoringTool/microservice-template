package cms.gov.madie.measure.exceptions;

public class InvalidResourceBundleStateException extends RuntimeException {
  private static final String MESSAGE = "Cannot bundle resource %s with ID %s while errors exist.";

  public InvalidResourceBundleStateException(String type, String id) {
    super(String.format(MESSAGE, type, id));
  }
}
