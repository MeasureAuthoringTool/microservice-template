package cms.gov.madie.measure.services;

import cms.gov.madie.measure.dto.PackageDto;
import cms.gov.madie.measure.dto.qrda.QrdaRequestDTO;
import gov.cms.madie.models.measure.Measure;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class QicorePackageService implements PackageService {
  private final BundleService bundleService;

  @Override
  public PackageDto getMeasurePackage(Measure measure, String accessToken) {
    return bundleService.getMeasureExport(measure, accessToken);
  }

  @Override
  public byte[] getQRDA(QrdaRequestDTO qrdaRequestDTO, String accessToken) {
    // to be implemented
    throw new UnsupportedOperationException("method not yet implemented");
  }
}
