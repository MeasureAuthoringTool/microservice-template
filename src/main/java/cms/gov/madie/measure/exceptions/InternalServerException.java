package cms.gov.madie.measure.exceptions;

public class InternalServerException extends RuntimeException {

  private static final long serialVersionUID = -4976407884786223809L;

  public InternalServerException(String message) {
    super(message);
  }
}
