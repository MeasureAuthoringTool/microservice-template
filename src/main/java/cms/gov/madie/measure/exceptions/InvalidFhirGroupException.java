package cms.gov.madie.measure.exceptions;

public class InvalidFhirGroupException extends RuntimeException {

  private static final long serialVersionUID = -4573591966439247541L;
  private static final String MESSAGE =
      "Measure Group Types and Population Basis are required for FHIR Measure Group";

  public InvalidFhirGroupException() {
    super(MESSAGE);
  }
}
