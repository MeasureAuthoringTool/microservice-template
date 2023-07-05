package cms.gov.madie.measure.exceptions;

public class ValidationServiceException extends RuntimeException {

  private static final long serialVersionUID = 6037320531517099699L;

  public ValidationServiceException(String message) {
    super(message);
  }
}
