package cms.gov.madie.measure.services;

import gov.cms.madie.models.measure.Measure;

public interface PackageService {
  byte[] getMeasurePackage(Measure measure, String accessToken);

  byte[] getQRDA(Measure measure, String accessToken);
}
