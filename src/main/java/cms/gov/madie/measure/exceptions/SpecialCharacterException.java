package cms.gov.madie.measure.exceptions;

public class SpecialCharacterException extends RuntimeException {
  private static final long serialVersionUID = 4113761000508937686L;

  private static final String MESSAGE =
      "Test Case %s can not contain special characters: (){}[]<>/|\"':;,.~`!@#$%^&*_+=\\";

  public SpecialCharacterException(String type) {
    super(MESSAGE.replace("%s", type));
  }
}
