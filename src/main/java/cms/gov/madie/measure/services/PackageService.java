package cms.gov.madie.measure.services;

import gov.cms.madie.models.measure.Measure;
import org.springframework.http.ResponseEntity;

public interface PackageService {
  byte[] getMeasurePackage(Measure measure, String accessToken);

  ResponseEntity<byte[]> getQRDA(Measure measure, String accessToken);
}
