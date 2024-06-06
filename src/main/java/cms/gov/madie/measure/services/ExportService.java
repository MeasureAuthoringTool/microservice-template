package cms.gov.madie.measure.services;

import cms.gov.madie.measure.dto.PackageDto;
import cms.gov.madie.measure.dto.qrda.QrdaRequestDTO;
import cms.gov.madie.measure.exceptions.InvalidResourceStateException;
import cms.gov.madie.measure.factories.ModelValidatorFactory;
import cms.gov.madie.measure.factories.PackageServiceFactory;
import cms.gov.madie.measure.utils.MeasureUtil;
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
  private final MeasureUtil measureUtil;

  public PackageDto getMeasureExport(Measure measure, String accessToken) {
    validateMetadata(measure);

    ModelValidator modelValidator =
        modelValidatorFactory.getModelValidator(ModelType.valueOfName(measure.getModel()));
    measure = measureUtil.validateAllMeasureDependencies(measure);
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

  public ResponseEntity<byte[]> getQRDA(QrdaRequestDTO qrdaRequestDTO, String accessToken) {
    if (CollectionUtils.isEmpty(qrdaRequestDTO.getMeasure().getTestCases())) {
      throw new InvalidResourceStateException(
          "Measure",
          qrdaRequestDTO.getMeasure().getId(),
          "since there are no test cases in the measure.");
    }
    PackageService packageService =
        packageServiceFactory.getPackageService(
            ModelType.valueOfName(qrdaRequestDTO.getMeasure().getModel()));
    return packageService.getQRDA(qrdaRequestDTO, accessToken);
  }
}
