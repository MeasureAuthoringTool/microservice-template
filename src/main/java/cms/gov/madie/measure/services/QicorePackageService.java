package cms.gov.madie.measure.services;

import cms.gov.madie.measure.dto.PackageDto;
import cms.gov.madie.measure.dto.qrda.QrdaRequestDTO;
import cms.gov.madie.measure.exceptions.BundleOperationException;
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
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class QicorePackageService implements PackageService {

  private final BundleService bundleService;
  private FhirServicesClient fhirServicesClient;
  private final ModelValidatorFactory modelValidatorFactory;
  private final MeasureUtil measureUtil;
  private final ExportRepository exportRepository;

  @Override
  public PackageDto getMeasurePackage(Measure measure, String accessToken) {
    return bundleService.getMeasureExport(measure, accessToken);
  }

  @Override
  public String getHumanReadable(Measure measure, String username, String accessToken) {
    String humanReadableWithCss = null;
    Export export = null;
    // versioned QI-Core measures will have human readable saved in Export
    if (measure.getMeasureMetaData() != null && !measure.getMeasureMetaData().isDraft()) {
      Optional<Export> savedExport = exportRepository.findByMeasureId(measure.getId());
      if (savedExport.isPresent()) {
        if (!StringUtils.isBlank(savedExport.get().getHumanReadable())) {
          return savedExport.get().getHumanReadable();
        } else {
          export = savedExport.get();
        }
      }
    }

    Measure existingMeasure = validateMeasure(measure);

    String measureBundle =
        fhirServicesClient.getMeasureBundle(existingMeasure, accessToken, "export");

    humanReadableWithCss = getHRWithCSS(existingMeasure, measureBundle);
    // to prevent duplicate save
    if (export != null) {
      export.setHumanReadable(humanReadableWithCss);
      Export savedExport = exportRepository.save(export);
      log.info(
          "User [{}] saved human readable with CSS in Export with ID [{}]",
          username,
          savedExport.getId());
    }
    log.info("User [{}] get human readable with ID [{}]", username, measure.getId());
    return humanReadableWithCss;
  }

  @Override
  public byte[] getQRDA(QrdaRequestDTO qrdaRequestDTO, String accessToken) {
    // to be implemented
    throw new UnsupportedOperationException("method not yet implemented");
  }

  protected Measure validateMeasure(Measure measure) {
    Measure validatedMeasure = measureUtil.validateAllMeasureDependencies(measure);

    ModelValidator modelValidator =
        modelValidatorFactory.getModelValidator(ModelType.valueOfName(measure.getModel()));
    modelValidator.validateMetadata(measure);
    modelValidator.validateGroups(validatedMeasure);
    modelValidator.validateCqlErrors(validatedMeasure);
    return validatedMeasure;
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
