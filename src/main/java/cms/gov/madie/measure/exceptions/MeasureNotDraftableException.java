package cms.gov.madie.measure.exceptions;

public class MeasureNotDraftableException extends RuntimeException {
  private static final String ONE_DRAFT_PER_SET =
      "Can not create a draft for the measure \"%s\". Only one draft is permitted per measure.";

  private static final String DUPLICATE_MEASURE =
      "Can not create a draft for the measure \"%s\"."
          + " Measure already exists with measure name \"%s\".";

  public MeasureNotDraftableException(String measureName) {
    super(String.format(ONE_DRAFT_PER_SET, measureName));
  }

  public MeasureNotDraftableException(String measureName, String draftName) {
    super(String.format(DUPLICATE_MEASURE, measureName, draftName));
  }
}
