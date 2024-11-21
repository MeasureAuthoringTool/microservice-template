package cms.gov.madie.measure.services;

import org.springframework.stereotype.Service;

/** Intentionally empty class. QI-Core 6 packaging is the same as QI-Core 4.1.1. */
@Service
public class Qicore6PackageService extends QicorePackageService {
  public Qicore6PackageService(BundleService bundleService) {
    super(bundleService);
  }
}
