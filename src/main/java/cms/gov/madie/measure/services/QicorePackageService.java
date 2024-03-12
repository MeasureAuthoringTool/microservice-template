package cms.gov.madie.measure.services;

import gov.cms.madie.models.measure.Measure;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class QicorePackageService implements PackageService {
  private final BundleService bundleService;

  @Override
  public byte[] getMeasurePackage(Measure measure, String accessToken) {
    return bundleService.getMeasureExport(measure, accessToken);
  }

  @Override
  public byte[] getQRDA(Measure measure, String accessToken) {
    // to be implemented
    return "".getBytes();
  }
}
