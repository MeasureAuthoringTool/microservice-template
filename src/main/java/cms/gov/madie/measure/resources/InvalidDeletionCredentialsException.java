package cms.gov.madie.measure.resources;

import lombok.Getter;

@Getter
public class InvalidDeletionCredentialsException extends RuntimeException {
  private static final String MESSAGE = "User: %s has invalid deletion credentials";

  public InvalidDeletionCredentialsException(String username) {
    super(String.format(MESSAGE, username));
  }
}
