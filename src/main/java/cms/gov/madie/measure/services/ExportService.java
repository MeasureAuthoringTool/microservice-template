package cms.gov.madie.measure.services;

import cms.gov.madie.measure.dto.PackageDto;
import cms.gov.madie.measure.exceptions.InvalidResourceStateException;
import cms.gov.madie.measure.factories.ModelValidatorFactory;
import cms.gov.madie.measure.factories.PackageServiceFactory;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.measure.Measure;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@AllArgsConstructor
public class ExportService {
  private final PackageServiceFactory packageServiceFactory;
  private final ModelValidatorFactory modelValidatorFactory;

  public PackageDto getMeasureExport(Measure measure, String accessToken) {
    validateMetadata(measure);
    ModelValidator modelValidator =
        modelValidatorFactory.getModelValidator(ModelType.valueOfName(measure.getModel()));
    modelValidator.validateGroups(measure);
    modelValidator.validateCqlErrors(measure);
    PackageService packageService =
        packageServiceFactory.getPackageService(ModelType.valueOfName(measure.getModel()));
    return packageService.getMeasurePackage(measure, accessToken);
  }

  // TODO: if there are significant diffs between QDM & QICore validations, move this to respective
  // model validators
  private void validateMetadata(Measure measure) {
    if (measure.getMeasureMetaData() != null) {
      if (CollectionUtils.isEmpty(measure.getMeasureMetaData().getDevelopers())) {
        throw new InvalidResourceStateException(
            "Measure", measure.getId(), "since there are no associated developers in metadata.");
      } else if (measure.getMeasureMetaData().getSteward() == null) {
        throw new InvalidResourceStateException(
            "Measure", measure.getId(), "since there is no associated steward in metadata.");
      } else if (StringUtils.isBlank(measure.getMeasureMetaData().getDescription())) {
        throw new InvalidResourceStateException(
            "Measure", measure.getId(), "since there is no description in metadata.");
      }
    }
  }

  public ResponseEntity<byte[]> getQRDA(Measure measure, String accessToken) {
    if (CollectionUtils.isEmpty(measure.getTestCases())) {
      throw new InvalidResourceStateException(
          "Measure", measure.getId(), "since there are no test cases in the measure.");
    }
    ModelValidator modelValidator =
        modelValidatorFactory.getModelValidator(ModelType.valueOfName(measure.getModel()));
    modelValidator.validateGroups(measure);
    PackageService packageService =
        packageServiceFactory.getPackageService(ModelType.valueOfName(measure.getModel()));
    return packageService.getQRDA(measure, accessToken);
  }
}
