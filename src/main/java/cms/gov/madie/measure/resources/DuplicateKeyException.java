package cms.gov.madie.measure.resources;

import lombok.Getter;

@Getter
public class DuplicateKeyException extends org.springframework.dao.DuplicateKeyException {
  private final String key;

  public DuplicateKeyException(String key) {
    super("Key should not have duplicate value: " + key);
    this.key = key;
  }
}
