package cms.gov.madie.measure.services;

import cms.gov.madie.measure.dto.PackageDto;
import cms.gov.madie.measure.dto.qrda.QrdaRequestDTO;
import gov.cms.madie.models.measure.Measure;

public interface PackageService {
  PackageDto getMeasurePackage(Measure measure, String accessToken);

  byte[] getQRDA(QrdaRequestDTO serviceDTO, String accessToken);
}
