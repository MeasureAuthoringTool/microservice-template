package cms.gov.madie.measure.exceptions;

public class ResourceNotFoundException extends RuntimeException {
  private static final String MESSAGE = "Could not find %s with id: %s";

  public ResourceNotFoundException(String type, String id) {
    super(String.format(MESSAGE, type, id));
  }
}
