package cms.gov.madie.measure.exceptions;

public class InvalidCmsIdException extends RuntimeException {

  private static final long serialVersionUID = -5015840088063095463L;
  private static final String MESSAGE = "Invalid CMS ID: %s, CMS ID is readonly";

  public InvalidCmsIdException(String changedCmsId) {
    super(String.format(MESSAGE, changedCmsId));
  }
}
