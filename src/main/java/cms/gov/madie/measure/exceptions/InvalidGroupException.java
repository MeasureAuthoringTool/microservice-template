package cms.gov.madie.measure.exceptions;

public class InvalidGroupException extends RuntimeException {

  private static final long serialVersionUID = -3841571888653750392L;

  public InvalidGroupException(String message) {
    super(message);
  }
}
