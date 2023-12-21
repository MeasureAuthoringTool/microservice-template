package cms.gov.madie.measure.factories;

import cms.gov.madie.measure.exceptions.UnsupportedTypeException;
import cms.gov.madie.measure.services.PackageService;
import gov.cms.madie.models.common.ModelType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PackageServiceFactory {

  private final Map<String, PackageService> packageServiceMap;

  public PackageServiceFactory(Map<String, PackageService> packageServiceMap) {
    this.packageServiceMap = packageServiceMap;
  }

  public PackageService getPackageService(ModelType modelType) {

    PackageService packageService =
        packageServiceMap.get(modelType.getShortValue() + "PackageService");
    if (packageService == null) {
      throw new UnsupportedTypeException(this.getClass().getName(), modelType.toString());
    }

    return packageService;
  }
}
