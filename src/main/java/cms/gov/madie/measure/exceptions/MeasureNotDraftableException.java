package cms.gov.madie.measure.exceptions;

public class MeasureNotDraftableException extends RuntimeException {
  private static final String MESSAGE =
      "Can not create a draft for the measure \"%s\". Only one draft is permitted per measure.";

  public MeasureNotDraftableException(String measureName) {
    super(String.format(MESSAGE, measureName));
  }
}
