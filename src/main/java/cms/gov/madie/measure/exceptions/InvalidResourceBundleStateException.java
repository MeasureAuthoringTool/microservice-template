package cms.gov.madie.measure.exceptions;

public class InvalidResourceBundleStateException extends RuntimeException {
  private static final String MESSAGE = "Cannot bundle resource %s with ID %s while %s";

  public InvalidResourceBundleStateException(String type, String id, String cause) {
    super(String.format(MESSAGE, type, id, cause));
  }
}
