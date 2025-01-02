package cms.gov.madie.measure.services;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import cms.gov.madie.measure.exceptions.CqlElmTranslationErrorException;
import cms.gov.madie.measure.exceptions.InvalidResourceStateException;
import gov.cms.madie.models.measure.ElmJson;
import gov.cms.madie.models.measure.Measure;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
@Service
public class ElmToJsonService {
  private final ElmTranslatorClient elmTranslatorClient;

  protected void retrieveElmJson(Measure measure, String accessToken) {
    if (StringUtils.isBlank(measure.getCql())) {
      throw new InvalidResourceStateException(
          "Measure", measure.getId(), "since there is no associated CQL.");
    }

    if (measure.isCqlErrors()) {
      throw new InvalidResourceStateException(
          "Measure", measure.getId(), "since CQL errors exist.");
    }

    if (CollectionUtils.isEmpty(measure.getGroups())) {
      throw new InvalidResourceStateException(
          "Measure", measure.getId(), "since there are no associated population criteria.");
    }

    final ElmJson elmJson =
        elmTranslatorClient.getElmJson(measure.getCql(), measure.getModel(), accessToken);
    if (elmTranslatorClient.hasErrors(elmJson)) {
      throw new CqlElmTranslationErrorException(measure.getMeasureName());
    }
    measure.setElmJson(elmJson.getJson());
    measure.setElmXml(elmJson.getXml());
  }
}
