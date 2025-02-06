package cms.gov.madie.measure.exceptions;

public class HarpIdMismatchException extends RuntimeException {

  private static final String MESSAGE =
      "Response could not be completed because the HARP id of %s passed in does not "
          + "match the owner of the measure with the measure id of %s. The owner of "
          + "the measure is %s";

  public HarpIdMismatchException(String harpId, String owner, String measureId) {
    super(String.format(MESSAGE, harpId, measureId, owner));
  }
}
