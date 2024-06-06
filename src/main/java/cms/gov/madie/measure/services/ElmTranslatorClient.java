package cms.gov.madie.measure.services;

import cms.gov.madie.measure.config.ElmTranslatorClientConfig;
import cms.gov.madie.measure.exceptions.CqlElmTranslationServiceException;
import gov.cms.madie.models.common.ModelType;
import gov.cms.madie.models.measure.ElmJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Slf4j
@Service
@AllArgsConstructor
public class ElmTranslatorClient {

  private ElmTranslatorClientConfig elmTranslatorClientConfig;
  private RestTemplate elmTranslatorRestTemplate;

  public ElmJson getElmJson(final String cql, String measureModel, String accessToken) {
    try {
      URI uri = getElmJsonURI(measureModel, false);
      HttpEntity<String> cqlEntity = getCqlHttpEntity(cql, accessToken, null, null);
      return elmTranslatorRestTemplate
          .exchange(uri, HttpMethod.PUT, cqlEntity, ElmJson.class)
          .getBody();
    } catch (Exception ex) {
      log.error("An error occurred calling the CQL to ELM translation service", ex);
      throw new CqlElmTranslationServiceException(
          "There was an error calling CQL-ELM translation service", ex);
    }
  }

  public boolean hasErrors(ElmJson elmJson) {
    if (elmJson == null) {
      return true;
    }
    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode jsonNode = mapper.readTree(elmJson.getJson());
      return jsonNode.has("errorExceptions") && jsonNode.get("errorExceptions").size() > 0;
    } catch (Exception ex) {
      log.error("An error occurred parsing the response from the CQL-ELM translation service", ex);
      throw new CqlElmTranslationServiceException(
          "There was an error calling CQL-ELM translation service", ex);
    }
  }

  public ElmJson getElmJsonForMatMeasure(
      final String cql, String measureModel, String apiKey, String harpId) {
    try {
      URI uri = getElmJsonURI(measureModel, true);
      HttpEntity<String> cqlEntity = getCqlHttpEntity(cql, null, apiKey, harpId);

      return elmTranslatorRestTemplate
          .exchange(uri, HttpMethod.PUT, cqlEntity, ElmJson.class)
          .getBody();
    } catch (Exception ex) {
      throw new CqlElmTranslationServiceException(
          "There was an error calling CQL-ELM translation service for MAT transferred measure", ex);
    }
  }

  protected URI getElmJsonURI(String measureModel, boolean isForMatTransferred) {
    var isQdm = StringUtils.equals(measureModel, ModelType.QDM_5_6.getValue());
    String baseUrl =
        isQdm
            ? elmTranslatorClientConfig.getQdmCqlElmServiceBaseUrl()
            : elmTranslatorClientConfig.getFhirCqlElmServiceBaseUrl();
    return URI.create(
        baseUrl
            + (isForMatTransferred
                ? elmTranslatorClientConfig.getCqlElmServiceUriForMatTransferredMeasure()
                : elmTranslatorClientConfig.getCqlElmServiceElmJsonUri()));
  }

  protected HttpEntity<String> getCqlHttpEntity(
      final String cql, String accessToken, String apiKey, String harpId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.TEXT_PLAIN);
    if (accessToken != null) {
      headers.set(HttpHeaders.AUTHORIZATION, accessToken);
    } else if (apiKey != null && harpId != null) {
      headers.set("api-key", apiKey);
      headers.set("harp-id", harpId);
    }
    return new HttpEntity<>(cql, headers);
  }
}
