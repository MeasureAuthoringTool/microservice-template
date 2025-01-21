package cms.gov.madie.measure.services;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;

import cms.gov.madie.measure.exceptions.BundleOperationException;
import cms.gov.madie.measure.exceptions.ResourceNotFoundException;
import cms.gov.madie.measure.exceptions.UnsupportedTypeException;
import cms.gov.madie.measure.factories.ModelValidatorFactory;
import cms.gov.madie.measure.repositories.ExportRepository;
import cms.gov.madie.measure.utils.MeasureUtil;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.measure.Export;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.packaging.utils.PackagingUtility;
import gov.cms.madie.packaging.utils.PackagingUtilityFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
@Service
public class HumanReadableService {

  private FhirServicesClient fhirServicesClient;
  private MeasureService measureService;
  private final ModelValidatorFactory modelValidatorFactory;
  private final MeasureUtil measureUtil;
  private final ExportRepository exportRepository;

  public String getHumanReadableWithCSS(String measureId, String username, String accessToken) {
    Measure measure = measureService.findMeasureById(measureId);
    if (measure == null) {
      throw new ResourceNotFoundException("Measure", measureId);
    }

    // this is temporary until we allow  to get QDM measures HR
    if (!measure.getModel().contains("QI-Core")) {
      throw new UnsupportedTypeException(this.getClass().getName(), measure.getModel().toString());
    }

    return getFhirHR(measure, username, accessToken);
  }

  protected String getFhirHR(Measure measure, String username, String accessToken) {
    String humanReadableWithCss = null;
    boolean saveHR = false;
    // versioned QI-Core measures will have human readable saved in Export
    if (measure.getMeasureMetaData() != null && !measure.getMeasureMetaData().isDraft()) {
      Optional<Export> savedExport = exportRepository.findByMeasureId(measure.getId());
      if (savedExport.isPresent()) {
        humanReadableWithCss = savedExport.get().getHumanReadable();
        if (!StringUtils.isBlank(humanReadableWithCss)) {
          return humanReadableWithCss;
        } else {
          saveHR = true;
        }
      } else {
        saveHR = true;
      }
    }

    measure = validateMeasure(measure);

    String measureBundle = fhirServicesClient.getMeasureBundle(measure, accessToken, "export");

    humanReadableWithCss = getHRWithCSS(measure, measureBundle);
    if (saveHR) {
      Export export =
          Export.builder()
              .measureId(measure.getId())
              .measureBundleJson(measureBundle)
              .humanReadable(humanReadableWithCss)
              .build();
      Export savedExport = exportRepository.save(export);
      log.info(
          "User [{}] saved human readable with CSS in Export with ID [{}]",
          username,
          savedExport.getId());
    }
    log.info("User [{}] get human readable with ID [{}]", username, measure.getId());
    return humanReadableWithCss;
  }

  protected Measure validateMeasure(Measure measure) {
    measureUtil.validateMetadata(measure);

    ModelValidator modelValidator =
        modelValidatorFactory.getModelValidator(ModelType.valueOfName(measure.getModel()));

    measure = measureUtil.validateAllMeasureDependencies(measure);
    modelValidator.validateGroups(measure);
    modelValidator.validateCqlErrors(measure);
    return measure;
  }

  protected String getHRWithCSS(Measure measure, String measureBundle) {
    String humanReadableWithCss = null;
    try {
      PackagingUtility utility = PackagingUtilityFactory.getInstance(measure.getModel());
      humanReadableWithCss = utility.getHumanReadableWithCSS(measureBundle);
    } catch (InstantiationException
        | IllegalAccessException
        | InvocationTargetException
        | NoSuchMethodException
        | ClassNotFoundException e) {
      throw new BundleOperationException("Measure", measure.getId(), e);
    }
    return humanReadableWithCss;
  }
}
