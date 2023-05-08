package cms.gov.madie.measure.utils;

import cms.gov.madie.measure.exceptions.InternalServerException;
import gov.cms.madie.models.measure.Export;

public interface ResourceUtility {

  byte[] getZipBundle(Export export, String exportFileName) throws InternalServerException;
}
