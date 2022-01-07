package cms.gov.madie.measure.resources;

import lombok.Getter;

@Getter
public class InvalidModelTypeException extends RuntimeException {

  private static final long serialVersionUID = -5144864659700899562L;
  private String key;

  public InvalidModelTypeException(String key, String message) {
    super(message);
    this.key = key;
  }
}
