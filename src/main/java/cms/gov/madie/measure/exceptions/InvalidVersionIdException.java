package cms.gov.madie.measure.exceptions;

public class InvalidVersionIdException extends RuntimeException {
  /** */
  private static final long serialVersionUID = 2702456822656981638L;

  private static final String MESSAGE = "Invalid Version ID: %s, Version ID is readonly";

  public InvalidVersionIdException(String changedVersionId) {
    super(String.format(MESSAGE, changedVersionId));
  }
}
