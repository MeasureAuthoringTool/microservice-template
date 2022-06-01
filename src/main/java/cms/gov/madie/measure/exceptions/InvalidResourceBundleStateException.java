package cms.gov.madie.measure.exceptions;

public class InvalidResourceBundleStateException extends RuntimeException {
  private static final String MESSAGE = "Response could not be completed for %s with ID %s, %s";

  public InvalidResourceBundleStateException(String type, String id, String cause) {
    super(String.format(MESSAGE, type, id, cause));
  }
}
