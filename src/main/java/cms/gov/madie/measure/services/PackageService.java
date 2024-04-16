package cms.gov.madie.measure.services;

import cms.gov.madie.measure.dto.PackageDto;
import gov.cms.madie.models.measure.Measure;

public interface PackageService {
  PackageDto getMeasurePackage(Measure measure, String accessToken);

  byte[] getQRDA(Measure measure, String accessToken);
}
