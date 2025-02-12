package cms.gov.madie.measure.services;

import cms.gov.madie.measure.factories.ModelValidatorFactory;
import cms.gov.madie.measure.factories.PackageServiceFactory;
import cms.gov.madie.measure.utils.MeasureUtil;

import org.springframework.stereotype.Service;

import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.measure.Measure;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
@Service
public class HumanReadableService {

  private MeasureService measureService;
  private final PackageServiceFactory packageServiceFactory;
  private final ModelValidatorFactory modelValidatorFactory;
  private final MeasureUtil measureUtil;

  public String getHumanReadableWithCSS(String measureId, String username, String accessToken) {
    Measure measure = measureService.findMeasureById(measureId);
    if (measure == null) {
      throw new ResourceNotFoundException("Measure", measureId);
    }

    PackageService packageService =
        packageServiceFactory.getPackageService(ModelType.valueOfName(measure.getModel()));
    if (measure.getMeasureMetaData() != null && !measure.getMeasureMetaData().isDraft()) {
      return packageService.getHumanReadableForVersionedMeasure(measure, username, accessToken);
    } else {
      ModelValidator modelValidator =
          modelValidatorFactory.getModelValidator(ModelType.valueOfName(measure.getModel()));
      measure = measureUtil.validateAllMeasureDependencies(measure);
      modelValidator.validateMetadata(measure);
      modelValidator.validateGroups(measure);
      modelValidator.validateCqlErrors(measure);
    }
    return packageService.getHumanReadable(measure, username, accessToken);
  }
}
