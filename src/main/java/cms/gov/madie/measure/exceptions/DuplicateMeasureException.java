package cms.gov.madie.measure.exceptions;

public class DuplicateMeasureException extends RuntimeException {
  private static final long serialVersionUID = 6725570083542826518L;

  private static final String MESSAGE = "The measure already exists";

  public DuplicateMeasureException() {
    super(MESSAGE);
  }
}
