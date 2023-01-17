package cms.gov.madie.measure.exceptions;

public class InternalServerErrorException extends Exception {

  private static final long serialVersionUID = 4099180699092491245L;

  public InternalServerErrorException(String message, Throwable cause) {
    super(message, cause);
  }
}
