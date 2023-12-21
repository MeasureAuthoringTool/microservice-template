package cms.gov.madie.measure.services;

import cms.gov.madie.measure.exceptions.InvalidResourceBundleStateException;
import cms.gov.madie.measure.factories.PackageServiceFactory;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.measure.Measure;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@AllArgsConstructor
public class ExportService {
  private final PackageServiceFactory packageServiceFactory;

  public byte[] getMeasureExport(Measure measure, String accessToken) {
    isMetadataValid(measure);
    areGroupsValid(measure);
    PackageService packageService =
        packageServiceFactory.getPackageService(ModelType.valueOfName(measure.getModel()));
    return packageService.getMeasurePackage(measure, accessToken);
  }

  private void areGroupsValid(Measure measure) {
    if (CollectionUtils.isEmpty(measure.getGroups())) {
      throw new InvalidResourceBundleStateException(
          "Measure", measure.getId(), "since there is no population criteria on the measure.");
    }
    if (measure.getGroups().stream()
        .anyMatch(g -> CollectionUtils.isEmpty(g.getMeasureGroupTypes()))) {

      throw new InvalidResourceBundleStateException(
          "Measure",
          measure.getId(),
          "since there is at least one Population Criteria with no type.");
    }
  }

  private void isMetadataValid(Measure measure) {
    if (measure.getMeasureMetaData() != null) {
      if (CollectionUtils.isEmpty(measure.getMeasureMetaData().getDevelopers())) {
        throw new InvalidResourceBundleStateException(
            "Measure", measure.getId(), "since there are no associated developers in metadata.");
      } else if (measure.getMeasureMetaData().getSteward() == null) {
        throw new InvalidResourceBundleStateException(
            "Measure", measure.getId(), "since there is no associated steward in metadata.");
      } else if (StringUtils.isBlank(measure.getMeasureMetaData().getDescription())) {

        throw new InvalidResourceBundleStateException(
            "Measure", measure.getId(), "since there is no description in metadata.");
      }
    }
  }
}
