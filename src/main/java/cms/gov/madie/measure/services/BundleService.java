package cms.gov.madie.measure.services;

import cms.gov.madie.measure.exceptions.BundleOperationException;
import cms.gov.madie.measure.exceptions.CqlElmTranslationErrorException;
import cms.gov.madie.measure.exceptions.CqlElmTranslationServiceException;
import cms.gov.madie.measure.exceptions.InvalidResourceBundleStateException;
import com.nimbusds.oauth2.sdk.util.StringUtils;
import gov.cms.madie.models.measure.ElmJson;
import gov.cms.madie.models.measure.Measure;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
@AllArgsConstructor
public class BundleService {

  private final FhirServicesClient fhirServicesClient;

  private final ElmTranslatorClient elmTranslatorClient;

  public String bundleMeasure(Measure measure, String accessToken) {
    if (measure == null) {
      return null;
    }

    if (measure.isCqlErrors()) {
      throw new InvalidResourceBundleStateException(
          "Measure", measure.getId(), "since CQL errors exist.");
    }

    if (CollectionUtils.isEmpty(measure.getGroups())) {
      throw new InvalidResourceBundleStateException(
          "Measure", measure.getId(), "since there are no associated measure groups.");
    }

    if (measure.getElmJson() == null || StringUtils.isBlank(measure.getElmJson())) {
      throw new InvalidResourceBundleStateException(
          "Measure", measure.getId(), "since there are issues with the CQL.");
    }

    try {
      final ElmJson elmJson = elmTranslatorClient.getElmJson(measure.getCql(), accessToken);
      if (elmTranslatorClient.hasErrors(elmJson)) {
        throw new CqlElmTranslationErrorException(measure.getMeasureName());
      }
      measure.setElmJson(elmJson.getJson());
      measure.setElmXml(elmJson.getXml());

      return fhirServicesClient.getMeasureBundle(measure, accessToken);
    } catch (CqlElmTranslationServiceException | CqlElmTranslationErrorException e) {
      throw e;
    } catch (Exception ex) {
      log.error("An error occurred while bundling measure {}", measure.getId(), ex);
      throw new BundleOperationException("Measure", measure.getId(), ex);
    }
  }
}
