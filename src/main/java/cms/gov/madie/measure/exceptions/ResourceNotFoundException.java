package cms.gov.madie.measure.exceptions;

public class ResourceNotFoundException extends RuntimeException {
  private static final String ID_MESSAGE = "Could not find %s with id: %s";

  public ResourceNotFoundException(String type, String id) {
    super(String.format(ID_MESSAGE, type, id));
  }
}
