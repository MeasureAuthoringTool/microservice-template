package cms.gov.madie.measure.exceptions;

public class InvalidDraftStatusException extends RuntimeException {

  private static final String MESSAGE =
      "Response could not be completed for measure with ID %s, since the measure is not in a draft"
          + " status";

  public InvalidDraftStatusException(String id) {
    super(String.format(MESSAGE, id));
  }
}
