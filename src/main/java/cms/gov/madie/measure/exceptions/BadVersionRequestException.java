package cms.gov.madie.measure.exceptions;

public class BadVersionRequestException extends RuntimeException {
  private static final long serialVersionUID = 5316139738075841057L;
  private static final String MESSAGE = "User %s cannot version %s with ID %s. %s";

  public BadVersionRequestException(String type, String id, String user, String reason) {
    super(String.format(MESSAGE, user, type, id, reason));
  }
}
