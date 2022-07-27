package cms.gov.madie.measure.exceptions;

public class CqlElmTranslationErrorException extends RuntimeException {
  private static final String MESSAGE =
      "CQL-ELM translator found errors in the CQL for measure %s!";

  public CqlElmTranslationErrorException(String libraryName) {
    super(String.format(MESSAGE, libraryName));
  }
}
