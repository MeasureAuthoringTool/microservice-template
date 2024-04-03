package cms.gov.madie.measure.exceptions;

public class MissingOrgException extends RuntimeException {
  private static final long serialVersionUID = -2986224464639480100L;
  private static final String MESSAGE = "No organizations are available while transferring \"%s\"";

  public MissingOrgException(String message) {
    super(String.format(MESSAGE, message));
  }
}
