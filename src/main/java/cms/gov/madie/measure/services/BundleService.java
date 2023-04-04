package cms.gov.madie.measure.services;

import cms.gov.madie.measure.exceptions.BundleOperationException;
import cms.gov.madie.measure.exceptions.CqlElmTranslationErrorException;
import cms.gov.madie.measure.exceptions.InvalidResourceBundleStateException;
import gov.cms.madie.models.measure.ElmJson;
import gov.cms.madie.models.measure.Measure;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@AllArgsConstructor
public class BundleService {

  private final FhirServicesClient fhirServicesClient;

  private final ElmTranslatorClient elmTranslatorClient;

  public String bundleMeasure(Measure measure, String accessToken, String bundleType) {
    if (measure == null) {
      return null;
    }

    try {
      retrieveElmJson(measure, accessToken);
      return fhirServicesClient.getMeasureBundle(measure, accessToken, bundleType);
    } catch (RestClientException | IllegalArgumentException ex) {
      log.error("An error occurred while bundling measure {}", measure.getId(), ex);
      throw new BundleOperationException("Measure", measure.getId(), ex);
    }
  }

  public ResponseEntity<byte[]> exportBundleMeasure(Measure measure, String accessToken) {
    if (measure == null) {
      return null;
    }
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
    try {
      retrieveElmJson(measure, accessToken);
      return fhirServicesClient.getMeasureBundleExport(measure, accessToken);
    } catch (RestClientException | IllegalArgumentException ex) {
      log.error("An error occurred while bundling measure {}", measure.getId(), ex);
      throw new BundleOperationException("Measure", measure.getId(), ex);
    }
  }

  protected void retrieveElmJson(Measure measure, String accessToken) {
    if (StringUtils.isBlank(measure.getCql())) {
      throw new InvalidResourceBundleStateException(
          "Measure", measure.getId(), "since there is no associated CQL.");
    }

    if (measure.isCqlErrors()) {
      throw new InvalidResourceBundleStateException(
          "Measure", measure.getId(), "since CQL errors exist.");
    }

    if (CollectionUtils.isEmpty(measure.getGroups())) {
      throw new InvalidResourceBundleStateException(
          "Measure", measure.getId(), "since there are no associated population criteria.");
    }

    final ElmJson elmJson = elmTranslatorClient.getElmJson(measure.getCql(), accessToken);
    if (elmTranslatorClient.hasErrors(elmJson)) {
      throw new CqlElmTranslationErrorException(measure.getMeasureName());
    }
    measure.setElmJson(elmJson.getJson());
    measure.setElmXml(elmJson.getXml());
  }
}
