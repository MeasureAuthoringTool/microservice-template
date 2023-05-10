package cms.gov.madie.measure.exceptions;

public class InvalidFhirGroup extends RuntimeException {

  private static final long serialVersionUID = -4573591966439247541L;
  private static final String MESSAGE =
      "Measure Group Types and Population Basis are required for FHIR Measure Group";

  public InvalidFhirGroup() {
    super(MESSAGE);
  }

  public InvalidFhirGroup(String message) {
    super(message);
  }
}
