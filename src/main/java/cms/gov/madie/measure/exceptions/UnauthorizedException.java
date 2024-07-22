package cms.gov.madie.measure.exceptions;

public class UnauthorizedException extends RuntimeException {
  private static final String MESSAGE = "User %s is not authorized for %s with ID %s";

  public UnauthorizedException(String type, String id, String user) {
    super(String.format(MESSAGE, user, type, id));
  }

  public UnauthorizedException(String message) {
    super(message);
  }
}
