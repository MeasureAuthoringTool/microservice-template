package cms.gov.madie.measure.services;

import cms.gov.madie.measure.dto.PackageDto;
import cms.gov.madie.measure.dto.QrdaRequestDTO;
import gov.cms.madie.models.measure.Measure;
import org.springframework.http.ResponseEntity;

public interface PackageService {
  PackageDto getMeasurePackage(Measure measure, String accessToken);

  ResponseEntity<byte[]> getQRDA(QrdaRequestDTO serviceDTO, String accessToken);
}
