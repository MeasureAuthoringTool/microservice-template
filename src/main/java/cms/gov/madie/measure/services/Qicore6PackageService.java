package cms.gov.madie.measure.services;

import cms.gov.madie.measure.factories.ModelValidatorFactory;
import cms.gov.madie.measure.repositories.ExportRepository;
import cms.gov.madie.measure.utils.MeasureUtil;
import org.springframework.stereotype.Service;

/** Intentionally empty class. QI-Core 6 packaging is the same as QI-Core 4.1.1. */
@Service
public class Qicore6PackageService extends QicorePackageService {
  public Qicore6PackageService(
      BundleService bundleService,
      FhirServicesClient fhirServicesClient,
      ModelValidatorFactory modelValidatorFactory,
      MeasureUtil measureUtil,
      ExportRepository exportRepository) {
    super(bundleService, fhirServicesClient, exportRepository);
  }
}
