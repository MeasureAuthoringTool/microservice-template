package cms.gov.madie.measure.exceptions;

public class CqmConversionException extends RuntimeException {
  private static final long serialVersionUID = 3845193288386262219L;

  public CqmConversionException(String message, Exception cause) {
    super(message, cause);
  }

  public CqmConversionException(String message) {
    super(message);
  }
}
